package com.infinite.gateway.common.enums;

public enum ResilienceEnum {

    RETRY("重试"),
    BREAKER("熔断"),
    FALLBACK("降级"),
    BULKHEAD("信号量隔离"),
    THREAD_POOL_BULKHEAD("线程池隔离");


    private final String des;

    ResilienceEnum(String des) {
        this.des = des;
    }
}
