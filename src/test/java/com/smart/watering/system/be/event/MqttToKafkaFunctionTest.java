package com.smart.watering.system.be.event;


import com.smart.watering.system.be.TelemetryApplication;
import com.smart.watering.system.be.base.BaseIntegrationTest;
import com.smart.watering.system.be.config.mqtt.MqttInbound;
import com.smart.watering.system.be.events.MqttWateringDataEventListener;
import com.smart.watering.system.be.metrics.MeterMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = TelemetryApplication.class)
class MqttToKafkaFunctionSpringTest extends BaseIntegrationTest {

    @Autowired
    private MqttWateringDataEventListener functions;
    @Autowired
    private MeterMetrics metrics;

    @Autowired
    private Flux<MqttInbound> mqttInboundFlux;

    @Test
    void mqttToKafka_setsKafkaKey_fromTopic() throws Exception {
        Message<String> in = MessageBuilder.withPayload("{\"type\":\"IOT_PLANT_EVENT\"}")
                .setHeader("mqtt_receivedTopic", "iot/plant/dev1/pot-03/events")
                .build();

        Flux<Message<String>> out = functions.mqttToKafka().apply(Flux.just(in));

        StepVerifier.create(out).assertNext(msg -> {
                    assertEquals(
                            "dev1:pot-03",
                            msg.getHeaders().get(KafkaHeaders.KEY)
                    );
                })
                .verifyComplete();
    }

    @Test
    void mqttSource_mapsInboundToMessage() {
        Supplier<Flux<Message<String>>> supplier =
                functions.mqttSource(mqttInboundFlux);

        StepVerifier.create(supplier.get())
                .assertNext(msg ->
                        org.junit.jupiter.api.Assertions.assertNotNull(
                                msg.getHeaders().get("mqttTopic")
                        )
                );
    }
}
