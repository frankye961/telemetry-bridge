package com.smart.watering.system.be.config.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.messaging.MessageChannel;

import java.util.Objects;

@Configuration
public class MqttIntegrationConfig {

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MqttConnectOptions mqttConnectOptions(MqttProps props) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] { props.host() });
        options.setCleanSession(true);

        if (props.username() != null && !props.username().isBlank()) {
            options.setUserName(props.username());
        }
        if (props.password() != null && !props.password().isBlank()) {
            options.setPassword(props.password().toCharArray());
        }
        return options;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttConnectOptions options) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageProducer mqttInbound(
            MqttPahoClientFactory factory,
            MqttProps props,
            MessageChannel mqttInputChannel) {

        String[] topics = props.subscriptions().stream()
                .map(Subscription::topic)
                .filter(Objects::nonNull)
                .toArray(String[]::new);

        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(props.clientId(), factory, topics);

        // QoS per-topic (Spring Integration supports int[] here)
        int[] qos = props.subscriptions().stream().mapToInt(Subscription::qos).toArray();
        adapter.setQos(qos);

        adapter.setOutputChannel(mqttInputChannel);
        adapter.setCompletionTimeout(5000);

        return adapter;
    }
}
