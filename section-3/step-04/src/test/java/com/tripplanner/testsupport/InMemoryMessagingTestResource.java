package com.tripplanner.testsupport;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

import java.util.HashMap;
import java.util.Map;

/**
 * Switches Flow messaging channels to the in-memory connector for tests.
 * More reliable than properties alone on CI runners where Kafka Dev Services
 * would otherwise start and break sink-based test harnesses.
 */
public class InMemoryMessagingTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        Map<String, String> props = new HashMap<>();
        props.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("flow-in", "flow-out-consumer"));
        props.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("flow-out", "flow-in-producer"));
        props.put("quarkus.kafka.devservices.enabled", "false");
        props.put("mp.messaging.incoming.flow-in.smallrye-in-memory.run-on-vertx-context", "true");
        props.put("mp.messaging.incoming.flow-out-consumer.smallrye-in-memory.run-on-vertx-context", "true");
        return props;
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}
