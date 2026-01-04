package com.smart.watering.system.be.config.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttSubscriberConfig {

    @Value("${mqtt.host}")
    private String host;
    @Value("${mqtt.port}")
    private int port;
    @Value("${mqtt.clientId}")
    private String clientId;
    @Value("${mqtt.topic}")
    private String topicFilter;

    @Bean
    public Mqtt5AsyncClient mqtt5AsyncClient(){
        return MqttClient.builder()
                .useMqttVersion5()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port)
                .buildAsync();
    }

    @Bean
    public MqttInboundFlux mqttInBoundFlux(){

    }
}
