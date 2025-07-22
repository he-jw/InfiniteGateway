package com.infinite.gateway.core.filter.route.resilience;

import com.infinite.gateway.common.enums.ResilienceEnum;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.route.resilience.fallback.FallbackHandler;
import com.infinite.gateway.core.filter.route.resilience.fallback.FallbackHandlerManager;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.asynchttpclient.Response;

import java.util.concurrent.*;
import java.util.function.Supplier;


public class Resilience {

    private static final Resilience INSTANCE = new Resilience();

    ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(10);

    private Resilience() {
    }

    public static Resilience getInstance() {
        return INSTANCE;
    }

    public Supplier<CompletionStage<Response>> build(GatewayContext gatewayContext, Supplier<CompletionStage<Response>> supplier) {
        RouteDefinition.ResilienceConfig resilienceConfig = gatewayContext.getRoute().getResilienceConfig();
        String serviceName = gatewayContext.getRequest().getServiceDefinition().getServiceName();
        for (ResilienceEnum resilienceEnum : resilienceConfig.getOrder()) {
            switch (resilienceEnum) {
                case RETRY -> {
                    Retry retry = ResilienceFactory.buildRetry(resilienceConfig, serviceName);
                    if (retry != null) {
                        supplier = Retry.decorateCompletionStage(retry, retryScheduler, supplier);
                    }
                }
                case FALLBACK -> {
                    if (resilienceConfig.isFallbackEnabled()) {
                        Supplier<CompletionStage<Response>> finalSupplier = supplier;
                        supplier = () ->
                                finalSupplier.get().exceptionally(throwable -> {
                                    FallbackHandlerManager
                                            .getFallbackHandler(resilienceConfig.getFallbackHandlerName())
                                            .handle(gatewayContext, throwable);
                                    return null;
                                });
                    }
                }
                case BREAKER -> {
                    CircuitBreaker circuitBreaker = ResilienceFactory.buildCircuitBreaker(resilienceConfig, serviceName);
                    if (circuitBreaker != null) {
                        supplier = CircuitBreaker.decorateCompletionStage(circuitBreaker, supplier);
                    }
                }
                case BULKHEAD -> {
                    Bulkhead bulkhead = ResilienceFactory.buildBulkHead(resilienceConfig, serviceName);
                    if (bulkhead != null) {
                        supplier = Bulkhead.decorateCompletionStage(bulkhead, supplier);
                    }
                }
                case THREAD_POOL_BULKHEAD -> {
                    ThreadPoolBulkhead threadPoolBulkhead = ResilienceFactory.buildThreadPoolBulkhead(resilienceConfig, serviceName);
                    if (threadPoolBulkhead != null) {
                        Supplier<CompletionStage<Response>> finalSupplier = supplier;
                        supplier = () -> threadPoolBulkhead.executeSupplier(() ->
                                finalSupplier.get().toCompletableFuture().join());
                    }
                }
            }
        }
        return supplier;
    }

}
