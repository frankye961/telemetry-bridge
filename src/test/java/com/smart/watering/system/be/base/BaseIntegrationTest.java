package com.smart.watering.system.be.base;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base integration test that starts a real Kafka broker via Testcontainers
 * and wires Spring Cloud Stream Kafka binder to it.
 *
 * Use this as a parent class for any @SpringBootTest that needs Kafka.
 */
@Testcontainers
@SpringBootTest
public class BaseIntegrationTest {

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka:latest"));

    @DynamicPropertySource
    static void registerKafkaProps(DynamicPropertyRegistry registry) {
        // Spring Cloud Stream Kafka binder
        registry.add("spring.cloud.stream.kafka.binder.brokers", KAFKA::getBootstrapServers);

        // (Optional) dev-friendly topic creation during tests
        registry.add("spring.cloud.stream.kafka.binder.auto-create-topics", () -> "true");

        // If anything in your app reads Spring Kafka directly (usually not needed with Stream):
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Test
    void contextLoads() {
        // If the context starts, Kafka connectivity is OK.
    }
}
