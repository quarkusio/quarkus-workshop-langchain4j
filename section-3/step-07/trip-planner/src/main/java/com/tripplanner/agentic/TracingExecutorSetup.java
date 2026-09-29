package com.tripplanner.agentic;

import dev.langchain4j.spi.ExecutorProvider;
import io.opentelemetry.context.Context;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.util.concurrent.Executor;

/**
 * Parallel agents run their branches on the executor LangChain4j gets from
 * {@link ExecutorProvider}. By default that executor does not carry the
 * OpenTelemetry context, so each branch would start its own trace. Wrapping
 * the Quarkus managed executor with {@link Context#taskWrapping(Executor)}
 * keeps every agent of one planning run in the same trace.
 */
@ApplicationScoped
public class TracingExecutorSetup {

    void onStart(@Observes StartupEvent event, ManagedExecutor managedExecutor) {
        Executor tracing = Context.taskWrapping(managedExecutor);
        ExecutorProvider.set(() -> tracing);
    }

    void onStop(@Observes ShutdownEvent event) {
        ExecutorProvider.set(null);
    }
}
