package com.integrity.candidate.config;

import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

/** Configures the reactive Kafka sender used to publish domain events. */
@Configuration
public class KafkaConfiguration {

  /** Builds the reactive Kafka sender from the configured bootstrap servers. */
  @Bean
  public KafkaSender<String, String> kafkaSender(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
    Map<String, Object> producerProperties =
        Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class,
            ProducerConfig.ACKS_CONFIG,
            "all");
    return KafkaSender.create(SenderOptions.create(producerProperties));
  }
}
