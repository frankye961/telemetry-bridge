package com.smart.watering.system.be.config.mqtt;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;

@Slf4j
@Configuration
@EnableConfigurationProperties(MqttProps.class)
public class MqttIntegrationConfig {

    @Bean
    public FluxMessageChannel mqttInputChannel() {
        return new FluxMessageChannel();
    }

    @Bean
    public MqttConnectOptions mqttConnectOptions(MqttProps props) {
        MqttConnectOptions options = new MqttConnectOptions();
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

        String baseClientId = (props.clientId() == null || props.clientId().isBlank())
                ? "telemetry-bridge"
                : props.clientId();

        String clientId = baseClientId + "-" + UUID.randomUUID();

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId, factory, topic);

        adapter.setQos(props.qos());
        adapter.setCompletionTimeout(5_000);
        adapter.setOutputChannel(mqttInputChannel);

        return adapter;
    }

    @Bean
    public Sinks.Many<MqttInbound> mqttInboundSink() {
        return Sinks.many().replay().limit(100);  // buffers last 100 events for late subscribers
    }

    @Bean
    public Flux<MqttInbound> mqttInboundFlux(Sinks.Many<MqttInbound> mqttInboundSink) {
        return mqttInboundSink.asFlux()
                .doOnSubscribe(s -> log.info("mqttInboundFlux SUBSCRIBED ✅"))
                .doOnNext(x -> log.info("mqttInboundFlux EMIT ✅ topic={}", x.topic()));
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttInputSubscriber(Sinks.Many<MqttInbound> mqttInboundSink) {
        return msg -> {
            String topic = String.valueOf(msg.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
            String payload = String.valueOf(msg.getPayload());

            log.info("MQTT RX topic={} payload={}", topic, payload);

            Sinks.EmitResult res = mqttInboundSink.tryEmitNext(new MqttInbound(topic, payload));
            if (res.isFailure()) {
                log.warn("MQTT emit failed: {}", res);
            }
        };
    }
}
