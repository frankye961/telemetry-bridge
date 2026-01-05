package com.smart.watering.system.be.config.mqtt;

public record Subscription(String topic, int qos) {}