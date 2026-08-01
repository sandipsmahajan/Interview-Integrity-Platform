package com.interviewintegrity.desktopclient.config;

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

/** Configures the reactive Kafka producer and consumer used by the desktop relay. */
@Configuration
public class KafkaConfiguration {

  /** Builds the Kafka sender used to ingest desktop payloads. */
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

  /** Builds the Kafka receiver used to relay platform topics to desktop clients. */
  @Bean
  public KafkaReceiver<String, String> kafkaReceiver(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
      @Value("${spring.kafka.consumer.group-id}") String groupId,
      @Value("${desktop.relay-topics}") java.util.List<String> topics) {
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
            "latest");
    ReceiverOptions<String, String> options =
        ReceiverOptions.<String, String>create(consumerProperties).subscription(topics);
    return KafkaReceiver.create(options);
  }
}
