package com.smart.watering.system.be.event;


import com.smart.watering.system.be.config.mqtt.MqttInbound;
import com.smart.watering.system.be.events.MqttWateringDataEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = MqttWateringDataEventListener.class)
class MqttToKafkaFunctionSpringTest {

    @Autowired
    private MqttWateringDataEventListener functions;

    @Autowired
    private Flux<MqttInbound> mqttInboundFlux;

    @Test
    void mqttToKafka_setsKafkaKey_fromTopic() throws Exception {
        Message<String> in = MessageBuilder.withPayload("{\"type\":\"IOT_PLANT_EVENT\"}")
                .setHeader("mqttTopic", "iot/plant/dev1/pot-03/events")
                .build();

        Flux<Message<String>> out = functions.mqttToKafka().apply(Flux.just(in));

        StepVerifier.create(out).assertNext(msg -> {
                    assertEquals(
                            "dev1:pot-03",
                            msg.getHeaders().get("kafkaKey")
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
