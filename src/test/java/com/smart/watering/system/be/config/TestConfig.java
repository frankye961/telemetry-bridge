package com.smart.watering.system.be.config;

import com.smart.watering.system.be.config.mqtt.MqttInbound;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
public class TestConfig {
    @Bean
    Flux<MqttInbound> mqttInboundFluxTest() {
        return Flux.just(
                new MqttInbound("iot/plant/dev1/pot-03/events", "{\"a\":1}")
        );
    }
}
