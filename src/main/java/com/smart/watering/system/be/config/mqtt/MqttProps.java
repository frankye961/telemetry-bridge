package com.smart.watering.system.be.config.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.mqtt")
public record MqttProps(
        String host,
        int port,
        String clientId,
        String username,
        String password,
        String topicFilter,
        String qos,
        List<Subscription> subscriptions
) {}
