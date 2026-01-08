package com.smart.watering.system.be.events;

import com.smart.watering.system.be.config.mqtt.MqttInbound;
import com.smart.watering.system.be.metrics.MeterMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Configuration
public class MqttWateringDataEventListener {

    private final MeterMetrics metrics;

    private static final String MQTT_TOPIC_HEADER = "mqttTopic";

    @Autowired
    public MqttWateringDataEventListener(MeterMetrics metrics) {
        this.metrics = metrics;
    }

    @Bean
    public Supplier<Flux<Message<String>>> mqttSource(Flux<MqttInbound> inbound) {
        return () -> inbound
                .doOnNext(msg -> {
                    log.info("MQTT in topic={} payload={}", msg.topic(), msg.payload());
                    metrics.incrementSuccessfulMessages();
                })
                .map(msg -> MessageBuilder.withPayload(msg.payload())
                        .setHeader(MQTT_TOPIC_HEADER, msg.topic())
                        .build())
                .doOnError(e -> {
                    log.error("Error in mqttSource stream reading", e);
                    metrics.incrementFailedMessages();
                })
                .onErrorResume(e -> Flux.empty());
    }

    @Bean
    public Function<Flux<Message<String>>, Flux<Message<String>>> mqttToKafka() {
        return inbound -> inbound
                .map(msg -> {
                    try {
                        log.info("Message processing {}", msg.getPayload());
                        return processMessage(msg);
                    } catch (Exception e) {
                        return sendToDlq(msg);
                    }
                });
    }

    private Message<String> processMessage(Message<String> inbound) {
        String topic = (String) inbound.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        String kafkaKey = extractKeyFromTopic(topic);
        log.info("logging payload incoming {}", inbound.getPayload());
        metrics.incrementSuccessfulMessages();
        return MessageBuilder.withPayload(inbound.getPayload())
                .copyHeaders(inbound.getHeaders())
                .setHeader(KafkaHeaders.KEY, kafkaKey)
                .build();
    }

    private Message<String> sendToDlq(Message<String> inbound) {
        metrics.incrementFailedMessages();
        return MessageBuilder.withPayload(inbound.getPayload())
                .copyHeaders(inbound.getHeaders())
                .build();
    }

    private static String extractKeyFromTopic(String topic) {
        if (topic == null) return "unknown:unknown";
        String[] p = topic.split("/");
        return (p.length >= 5) ? (p[2] + ":" + p[3]) : "unknown:unknown";
    }
}
