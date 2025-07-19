package com.infinite.gateway.core.filter.route.resilience;

import com.infinite.gateway.common.enums.CircuitBreakerEnum;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.core.manager.DynamicConfigManager;
import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResilienceFactory {

    private static final Map<String /* 服务名 */, Retry /* 重试 */> retryMap = new ConcurrentHashMap<>();
    private static final Map<String /* 服务名 */, ThreadPoolBulkhead /* 线程池隔离 */> threadPoolBulkheadMap = new ConcurrentHashMap<>();
    private static final Map<String /* 服务名 */, CircuitBreaker /* 熔断器 */> circuitBreakerMap = new ConcurrentHashMap<>();
    private static final Map<String /* 服务名 */, Bulkhead /* 信号量隔离 */> bulkheadMap = new ConcurrentHashMap<>();

    public static Retry buildRetry(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        if (!resilienceConfig.isRetryEnabled()) {
            return null;
        }
        return retryMap.computeIfAbsent(serviceName, name -> {
            DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> retryMap.remove(newRoute.getServiceName()));
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(resilienceConfig.getMaxAttempts())
                    .waitDuration(Duration.ofMillis(resilienceConfig.getWaitDuration()))
                    .build();
            return RetryRegistry.of(config).retry(serviceName);
        });
    }

    public static CircuitBreaker buildCircuitBreaker(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        if (!resilienceConfig.isCircuitBreakerEnabled()) {
            return null;
        }
        return circuitBreakerMap.computeIfAbsent(serviceName, name -> {
            DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> circuitBreakerMap.remove(newRoute.getServiceName()));
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(resilienceConfig.getFailureRateThreshold())
                    .slowCallRateThreshold(resilienceConfig.getSlowCallRateThreshold())
                    .waitDurationInOpenState(Duration.ofMillis(resilienceConfig.getWaitDurationInOpenState()))
                    .slowCallDurationThreshold(Duration.ofSeconds(resilienceConfig.getSlowCallDurationThreshold()))
                    .permittedNumberOfCallsInHalfOpenState(resilienceConfig.getPermittedNumberOfCallsInHalfOpenState())
                    .minimumNumberOfCalls(resilienceConfig.getMinimumNumberOfCalls())
                    .slidingWindowType(slidingWindowTypeConvert(resilienceConfig.getType()))
                    .slidingWindowSize(resilienceConfig.getSlidingWindowSize())
                    .build();
            return CircuitBreakerRegistry.of(circuitBreakerConfig).circuitBreaker(serviceName);
        });
    }

    public static Bulkhead buildBulkHead(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        if (!resilienceConfig.isBulkheadEnabled()) {
            return null;
        }
        return bulkheadMap.computeIfAbsent(serviceName, name -> {
            DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> bulkheadMap.remove(newRoute.getServiceName()));
            BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(resilienceConfig.getMaxConcurrentCalls())
                    .maxWaitDuration(Duration.ofMillis(resilienceConfig.getMaxWaitDuration()))
                    .fairCallHandlingStrategyEnabled(resilienceConfig.isFairCallHandlingEnabled()).build();
            return BulkheadRegistry.of(bulkheadConfig).bulkhead(serviceName);
        });
    }

    public static ThreadPoolBulkhead buildThreadPoolBulkhead(RouteDefinition.ResilienceConfig resilienceConfig, String serviceName) {
        if (!resilienceConfig.isThreadPoolBulkheadEnabled()) {
            return null;
        }
        return threadPoolBulkheadMap.computeIfAbsent(serviceName, name -> {
            DynamicConfigManager.getInstance().addRouteListener(serviceName, newRoute -> threadPoolBulkheadMap.remove(newRoute.getServiceName()));
            ThreadPoolBulkheadConfig threadPoolBulkheadConfig = ThreadPoolBulkheadConfig.custom()
                    .coreThreadPoolSize(resilienceConfig.getCoreThreadPoolSize())
                    .maxThreadPoolSize(resilienceConfig.getMaxThreadPoolSize())
                    .queueCapacity(resilienceConfig.getQueueCapacity())
                    .build();
            return ThreadPoolBulkheadRegistry.of(threadPoolBulkheadConfig).bulkhead(serviceName);
        });
    }

    private static CircuitBreakerConfig.SlidingWindowType slidingWindowTypeConvert(CircuitBreakerEnum from) {
        if (from == CircuitBreakerEnum.TIME_BASED) {
            return CircuitBreakerConfig.SlidingWindowType.TIME_BASED;
        } else {
            return CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
        }
    }

}
