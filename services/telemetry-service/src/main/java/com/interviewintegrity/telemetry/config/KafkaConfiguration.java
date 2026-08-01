package com.interviewintegrity.telemetry.config;

import com.interviewintegrity.event.KafkaTopics;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

/** Configures the reactive Kafka sender and receiver used by the telemetry service. */
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

  /** Builds a receiver subscribed to the telemetry event topic. */
  @Bean
  public KafkaReceiver<String, String> telemetryReceiver(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
      @Value("${spring.kafka.consumer.group-id:telemetry-service}") String groupId) {
    Map<String, Object> consumerProperties =
        Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG,
            groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest");
    ReceiverOptions<String, String> options =
        ReceiverOptions.<String, String>create(consumerProperties)
            .subscription(List.of(KafkaTopics.TELEMETRY_RECEIVED));
    return KafkaReceiver.create(options);
  }
}
