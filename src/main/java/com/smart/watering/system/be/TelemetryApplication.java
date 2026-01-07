package com.smart.watering.system.be;

import com.smart.watering.system.be.config.mqtt.MqttProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableWebFlux
@EnableConfigurationProperties(MqttProps.class)
@SpringBootApplication
public class TelemetryApplication {
    static void main(String[] args){
        SpringApplication.run(TelemetryApplication.class, args);
    }
}
