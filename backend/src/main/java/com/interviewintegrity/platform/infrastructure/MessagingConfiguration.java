package com.interviewintegrity.platform.infrastructure;

import com.interviewintegrity.platform.application.PlatformServices.ReactiveEventPublisher;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {
  @Bean
  SenderOptions<String, String> kafkaSenderOptions(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
    return SenderOptions.create(Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.ACKS_CONFIG, "all",
        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true));
  }

  @Bean
  KafkaSender<String, String> kafkaSender(SenderOptions<String, String> senderOptions) {
    return KafkaSender.create(senderOptions);
  }

  @Bean
  ReactiveEventPublisher reactiveEventPublisher(KafkaSender<String, String> sender) {
    return (topic, key, payload) -> sender
        .send(Mono.just(SenderRecord.create(topic, null, null, key, payload, null)))
        .then();
  }
}
