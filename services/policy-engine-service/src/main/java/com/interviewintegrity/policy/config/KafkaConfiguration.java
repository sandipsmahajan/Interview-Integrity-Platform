package com.interviewintegrity.policy.config;

import com.interviewintegrity.event.KafkaTopics;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

/** Configures the reactive Kafka receiver used by the policy engine service. */
@Configuration
public class KafkaConfiguration {

  /** Builds a receiver subscribed to the violation topic. */
  @Bean
  public KafkaReceiver<String, String> violationReceiver(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
      @Value("${spring.kafka.consumer.group-id:policy-engine-service}") String groupId) {
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
            .subscription(List.of(KafkaTopics.POLICY_VIOLATION));
    return KafkaReceiver.create(options);
  }
}
