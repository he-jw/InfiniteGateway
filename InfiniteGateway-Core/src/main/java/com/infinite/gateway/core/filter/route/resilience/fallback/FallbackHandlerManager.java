package com.infinite.gateway.core.filter.route.resilience.fallback;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class FallbackHandlerManager {

    private static volatile Map<String /* 服务名 */, FallbackHandler> fallbackHandlerMap;

    public static FallbackHandler getFallbackHandler(String mark) {
        if (fallbackHandlerMap == null) {
            synchronized (FallbackHandlerManager.class) {
                if (fallbackHandlerMap == null) {
                    loadFallbackHandler();
                }
            }
        }
        return fallbackHandlerMap.getOrDefault(mark, new DefaultFallbackHandler());
    }

    private static void loadFallbackHandler() {
        ServiceLoader<FallbackHandler> loader = ServiceLoader.load(FallbackHandler.class);
        Map<String, FallbackHandler> map = new HashMap<>();
        for (FallbackHandler fallbackHandler : loader) {
            map.put(fallbackHandler.mark(), fallbackHandler);
        }
        fallbackHandlerMap = map;
    }
}
