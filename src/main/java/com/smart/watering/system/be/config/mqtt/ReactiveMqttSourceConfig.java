package com.smart.watering.system.be.config.mqtt;


import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;

@Configuration
public class ReactiveMqttSourceConfig {

    public record MqttInbound(String topic, String payload) {
    }

    @Bean
    Mqtt3AsyncClient mqttClient(MqttProps props) {
        return MqttClient.builder()
                .useMqttVersion3()
                .identifier(props.clientId())
                .serverHost(props.brokerUri())
                .buildAsync();
    }

    @Bean
    public Flux<MqttInbound> mqttInboundFluxReactive(Mqtt3AsyncClient client, MqttProps props) {
        // Sink = bridge from callback-world to reactive-world
        Sinks.Many<MqttInbound> sink = Sinks.many().multicast().onBackpressureBuffer();
        var connection = client.connectWith().cleanSession(true);
        if (props.username() != null && !props.username().isBlank()) {
            connection.simpleAuth()
                    .username(props.username())
                    .password(props.password() == null ? new byte[0] : props.password().getBytes(StandardCharsets.UTF_8)).applySimpleAuth();
        }
        Mono<Void> connect = Mono.fromCompletionStage(connection.send()).then();

        Mono<Void> subscribe = Mono.fromCompletionStage(client.subscribeWith()
                .topicFilter(props.topic())
                .qos(MqttQos.AT_LEAST_ONCE) // e.g. "AT_LEAST_ONCE" only temporary, to understand and change
                .callback((Mqtt3Publish publish) -> {
                    String topic = publish.getTopic().toString();
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    sink.tryEmitNext(new MqttInbound(topic, payload));
                }).send()
        ).then();

        return connect
                .then(subscribe)
                .thenMany(sink.asFlux())
                .doOnCancel(client::disconnect)
                .retry();
    }
}
