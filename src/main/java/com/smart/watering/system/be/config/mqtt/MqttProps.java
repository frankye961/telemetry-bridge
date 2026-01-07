package com.smart.watering.system.be.config.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mqtt")
public record MqttProps(
        String brokerUri,
        String clientId,
        String username,
        String password,
        String topic,
        int qos
) {}
