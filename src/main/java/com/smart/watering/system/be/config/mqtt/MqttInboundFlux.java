package com.smart.watering.system.be.config.mqtt;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import org.springframework.beans.factory.DisposableBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;

public final class MqttInboundFlux implements DisposableBean {
    private final Sinks.Many<Mqtt5Publish> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Mqtt5AsyncClient client;

    public MqttInboundFlux(Mqtt5AsyncClient client, String topicFilter) {
        this.client = client;

        client.connect()
                .thenCompose(ack -> client.subscribeWith()
                        .topicFilter(topicFilter)
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .callback(publish -> sink.tryEmitNext(publish))
                        .send()
                );
    }

    public Flux<MqttMessage> flux() {
        return sink.asFlux()
                .map(pub -> new MqttMessage(
                        pub.getTopic().toString(),
                        pub.getPayloadAsBytes() == null ? "" : new String(pub.getPayloadAsBytes(), StandardCharsets.UTF_8)
                ));
    }

    @Override
    public void destroy() {
        client.disconnect();
    }
}
