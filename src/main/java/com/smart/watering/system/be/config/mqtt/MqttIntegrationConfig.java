package com.smart.watering.system.be.config.mqtt;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Configuration
@EnableConfigurationProperties(MqttProps.class)
public class MqttIntegrationConfig {

    /**
     * Reactive channel so you can consume MQTT as a Flux downstream (WebFlux-friendly).
     */
    @Bean
    public FluxMessageChannel mqttInputChannel() {
        return new FluxMessageChannel();
    }

    @Bean
    public MqttConnectOptions mqttConnectOptions(MqttProps props) {
        MqttConnectOptions options = new MqttConnectOptions();

        // Must include scheme: tcp://host:port or ssl://host:port
        options.setServerURIs(new String[]{props.brokerUri()});

        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

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
            MessageChannel mqttInputChannel
    ) {
        String topic = props.topic();
        if (topic == null || topic.isBlank()) {
            throw new IllegalStateException("Missing MQTT topic filter. Set app.mqtt.topic.");
        }

        // Avoid clientId collisions if you run multiple instances in dev
        String baseClientId = (props.clientId() == null || props.clientId().isBlank())
                ? "telemetry-bridge"
                : props.clientId();
        String clientId = baseClientId + "-" + UUID.randomUUID();

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, factory, topic);

        adapter.setQos(props.qos()); // 0/1/2
        adapter.setCompletionTimeout(5_000);
        adapter.setOutputChannel(mqttInputChannel);

        return adapter;
    }

    /**
     * Expose inbound MQTT messages as a Flux for your Supplier<Flux<...>> function.
     */
    @Bean
    public Flux<MqttInbound> mqttInboundFlux(FluxMessageChannel mqttInputChannel) {
        return Flux.from(mqttInputChannel)
                .map(msg -> new MqttInbound(
                        String.valueOf(msg.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)),
                        String.valueOf(msg.getPayload())
                ));
    }
}
