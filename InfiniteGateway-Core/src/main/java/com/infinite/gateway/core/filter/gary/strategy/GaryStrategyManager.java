package com.infinite.gateway.core.filter.gary.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class GaryStrategyManager {

    public static final Map<String, GrayStrategy> STRATEGY_MAP = new HashMap<>();

    static {
        ServiceLoader<GrayStrategy> loader = ServiceLoader.load(GrayStrategy.class);
        for (GrayStrategy strategy : loader) {
            STRATEGY_MAP.put(strategy.mark(), strategy);
        }
    }

    public static GrayStrategy getGrayStrategy(String mark) {
        return STRATEGY_MAP.get(mark);
    }

}
