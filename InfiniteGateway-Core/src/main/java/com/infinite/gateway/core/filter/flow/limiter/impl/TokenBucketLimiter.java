package com.infinite.gateway.core.filter.flow.limiter.impl;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.exception.LimitedException;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.flow.limiter.FlowLimiter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TokenBucketLimiter implements FlowLimiter {

    private final int capacityTokens;
    private final int produceRate;
    private final AtomicInteger avilableTokens;

    private TokenBucketLimiter(int capacityTokens, int produceRate) {
        this.capacityTokens = capacityTokens;
        this.produceRate = produceRate;
        this.avilableTokens = new AtomicInteger(0);
        startFill();
    }

    @Override
    public void limit(GatewayContext context) {
        if (avilableTokens.getAndDecrement() >= 0) {
            context.doFilter();
        } else {
            avilableTokens.incrementAndGet();
            throw new LimitedException(ResponseCode.TOO_MANY_REQUESTS);
        }
    }

    private void startFill() {
        ScheduledExecutorService produceExecutor = Executors.newSingleThreadScheduledExecutor();
        produceExecutor.scheduleAtFixedRate(
                () -> avilableTokens.compareAndSet(avilableTokens.get(), Math.min(capacityTokens, avilableTokens.get() + produceRate)),
                0,
                produceRate,
                TimeUnit.MILLISECONDS
        );
    }
}
