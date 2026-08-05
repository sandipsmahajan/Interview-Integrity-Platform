package com.integrity.notification.service;

import com.integrity.notification.config.MailProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Sends email through the configured SMTP transport.
 *
 * <p>The underlying {@link JavaMailSender} is blocking, so dispatch is moved onto the bounded
 * elastic scheduler to keep the reactive pipeline non-blocking. A {@link MailException} is
 * translated into a {@link EmailDispatchException} so callers can distinguish transport failures
 * from programming errors.
 */
public final class SmtpEmailDispatcher implements EmailDispatcher {

  private static final Logger log = LoggerFactory.getLogger(SmtpEmailDispatcher.class);

  private final JavaMailSender mailSender;
  private final String from;

  /** Creates a dispatcher bound to the mail sender and configured From address. */
  public SmtpEmailDispatcher(JavaMailSender mailSender, MailProperties mailProperties) {
    this.mailSender = mailSender;
    this.from = mailProperties.getFrom();
  }

  @Override
  public Mono<String> send(String to, String subject, String htmlBody, String plainText) {
    return Mono.fromCallable(() -> doSend(to, subject, htmlBody, plainText))
        .subscribeOn(Schedulers.boundedElastic());
  }

  private String doSend(String to, String subject, String htmlBody, String plainText) {
    try {
      var message = mailSender.createMimeMessage();
      var helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(from);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(plainText, htmlBody);
      mailSender.send(message);
      String messageId = message.getMessageID();
      log.info("Dispatched email to {} subject '{}' providerMessageId {}", to, subject, messageId);
      return messageId != null ? messageId : UUID.randomUUID().toString();
    } catch (MailException | jakarta.mail.MessagingException e) {
      throw new EmailDispatchException(to, subject, e);
    }
  }
}
