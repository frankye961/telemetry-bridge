package com.smart.watering.system.be.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MeterMetrics {

    private static final String METRICS_NAME = "telemetry-bridge";
    private final Counter succesfullMessages;
    private final Counter failedMessages;

    public MeterMetrics(MeterRegistry registry){
        succesfullMessages = Counter.builder(METRICS_NAME + "succesful_messages")
                .description("Succeful messages sent")
                .register(registry);
        failedMessages = Counter.builder(METRICS_NAME + "failed_messages")
                .description("failed messages sent")
                .register(registry);
    }

    public void incrementUserSuccessfulMessages(){
        succesfullMessages.increment();
    }

    public void incrementUserFailedMessages(){
        failedMessages.increment();
    }
}
