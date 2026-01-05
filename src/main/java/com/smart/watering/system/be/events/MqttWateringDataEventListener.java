package com.smart.watering.system.be.events;

import com.smart.watering.system.be.config.mqtt.MqttInbound;
import io.reactivex.functions.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Slf4j
@Configuration
public class MqttWateringDataEventListener {

    @Bean
    public Supplier<Flux<Message<String>>> mqttSource(Flux<MqttInbound> inbound) {
        return () -> inbound.map(msg -> {
            log.info("logging msg {}", msg.payload());
            return MessageBuilder.withPayload(msg.payload())
                    .setHeader(KafkaHeaders.TOPIC, msg.topic())
                    .build();
        });
    }

    @Bean
    public Function<Flux<Message<String>>, Flux<Message<String>>> mqttToKafka() {
        return inbound -> inbound.map(msg -> {
            String topic = (String) msg.getHeaders().get("mqttTopic");
            String kafkaKey = extractKeyFromTopic(topic);
            log.info("logging payload {}", msg.getPayload());
            return MessageBuilder.withPayload(msg.getPayload())
                    .copyHeaders(msg.getHeaders())
                    .setHeader(KafkaHeaders.KEY, kafkaKey)
                    .build();
        });
    }

    private static String extractKeyFromTopic(String topic) {
        if (topic == null) return "unknown:unknown";
        String[] p = topic.split("/");
        return (p.length >= 5) ? (p[2] + ":" + p[3]) : "unknown:unknown";
    }
}
