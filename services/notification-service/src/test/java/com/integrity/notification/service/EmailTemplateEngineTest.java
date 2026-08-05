package com.integrity.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.integrity.notification.domain.NotificationChannel;
import com.integrity.notification.domain.NotificationTemplate;
import com.integrity.notification.repository.NotificationTemplateRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Unit tests for email template resolution and rendering. */
@ExtendWith(MockitoExtension.class)
class EmailTemplateEngineTest {

  @Mock private NotificationTemplateRepository templateRepository;

  private EmailTemplateEngine engine;

  @BeforeEach
  void setUp() {
    engine = new EmailTemplateEngine(templateRepository);
  }

  @Test
  void substituteReplacesPlaceholdersAndLeavesUnknownKeysEmpty() {
    String rendered =
        EmailTemplateEngine.substitute("Hi {{name}}, code {{otpCode}}", Map.of("name", "Alice"));

    assertThat(rendered).isEqualTo("Hi Alice, code ");
  }

  @Test
  void toPlainTextStripsHtmlTags() {
    String plain = EmailTemplateEngine.toPlainText("<p>Hello <b>Alice</b></p><p>Second line</p>");

    assertThat(plain).contains("Hello Alice").contains("Second line");
  }

  @Test
  void renderPrefersTenantTemplate() {
    UUID organizationId = UUID.randomUUID();
    NotificationTemplate tenant =
        new NotificationTemplate(
            organizationId,
            "welcome",
            NotificationChannel.EMAIL,
            "Tenant subject",
            "<p>Hi {{name}}</p>",
            "en");
    when(templateRepository.findLiveByOrganizationCodeChannelLocale(
            eq(organizationId), eq("welcome"), eq(NotificationChannel.EMAIL), eq("en")))
        .thenReturn(Mono.just(tenant));
    when(templateRepository.findLivePlatformDefault(
            eq("welcome"), eq(NotificationChannel.EMAIL), eq("en")))
        .thenReturn(Mono.empty());

    StepVerifier.create(engine.render(organizationId, "welcome", "en", Map.of("name", "Alice")))
        .assertNext(
            rendered -> {
              assertThat(rendered.subject()).isEqualTo("Tenant subject");
              assertThat(rendered.htmlBody()).isEqualTo("<p>Hi Alice</p>");
              assertThat(rendered.plainText()).contains("Hi Alice");
            })
        .verifyComplete();
  }

  @Test
  void renderFallsBackToPlatformEnglishDefault() {
    UUID organizationId = UUID.randomUUID();
    NotificationTemplate platform =
        new NotificationTemplate(
            null,
            "welcome",
            NotificationChannel.EMAIL,
            "Platform subject",
            "<p>Hello {{name}}</p>",
            "en");
    when(templateRepository.findLiveByOrganizationCodeChannelLocale(
            any(), eq("welcome"), eq(NotificationChannel.EMAIL), eq("de")))
        .thenReturn(Mono.empty());
    when(templateRepository.findLivePlatformDefault(
            eq("welcome"), eq(NotificationChannel.EMAIL), eq("de")))
        .thenReturn(Mono.empty());
    when(templateRepository.findLivePlatformDefault(
            eq("welcome"), eq(NotificationChannel.EMAIL), eq("en")))
        .thenReturn(Mono.just(platform));

    StepVerifier.create(engine.render(organizationId, "welcome", "de", Map.of("name", "Bob")))
        .assertNext(rendered -> assertThat(rendered.subject()).isEqualTo("Platform subject"))
        .verifyComplete();
  }

  @Test
  void renderCompletesEmptyWhenNoTemplateMatches() {
    UUID organizationId = UUID.randomUUID();
    when(templateRepository.findLiveByOrganizationCodeChannelLocale(
            any(), eq("unknown"), eq(NotificationChannel.EMAIL), eq("en")))
        .thenReturn(Mono.empty());
    when(templateRepository.findLivePlatformDefault(
            eq("unknown"), eq(NotificationChannel.EMAIL), eq("en")))
        .thenReturn(Mono.empty());

    StepVerifier.create(engine.render(organizationId, "unknown", "en", Map.of())).verifyComplete();
  }
}
