package com.infinite.gateway.dynamic.thread.pool.registry;

import com.infinite.gateway.dynamic.thread.pool.spi.RejectedPolicyProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 拒绝策略注册表
 * 
 * <p>负责管理所有的拒绝策略（内置 + SPI 扩展）</p>
 * 
 * @author InfiniteGateway
 */
@Slf4j
public class RejectedPolicyRegistry {

    /**
     * 策略名称 -> RejectedExecutionHandler 映射
     */
    private static final Map<String, RejectedExecutionHandler> POLICY_MAP = new ConcurrentHashMap<>();

    /**
     * 是否已初始化
     */
    private static volatile boolean initialized = false;

    /**
     * 初始化注册表（加载内置策略 + SPI 扩展策略）
     */
    public static void init() {
        if (initialized) {
            return;
        }

        synchronized (RejectedPolicyRegistry.class) {
            if (initialized) {
                return;
            }

            // 1. 注册 JDK 内置策略
            registerBuiltInPolicies();

            // 2. 通过 SPI 加载扩展策略
            loadSpiPolicies();

            initialized = true;
            log.info("RejectedPolicyRegistry initialized with {} policies: {}", 
                    POLICY_MAP.size(), POLICY_MAP.keySet());
        }
    }

    /**
     * 注册 JDK 内置的拒绝策略
     */
    private static void registerBuiltInPolicies() {
        register("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy());
        register("AbortPolicy", new ThreadPoolExecutor.AbortPolicy());
        register("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy());
        register("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    /**
     * 通过 SPI 加载扩展策略
     */
    private static void loadSpiPolicies() {
        ServiceLoader<RejectedPolicyProvider> serviceLoader = ServiceLoader.load(RejectedPolicyProvider.class);
        
        for (RejectedPolicyProvider provider : serviceLoader) {
            try {
                String policyName = provider.getPolicyName();
                RejectedExecutionHandler handler = provider.createHandler();
                
                // 检查是否已存在同名策略
                if (POLICY_MAP.containsKey(policyName)) {
                    log.warn("Policy '{}' already exists, will be overridden by SPI provider: {}", 
                            policyName, provider.getClass().getName());
                }
                
                register(policyName, handler);
                log.info("Loaded SPI rejected policy: {} from {}", policyName, provider.getClass().getName());
                
            } catch (Exception e) {
                log.error("Failed to load SPI rejected policy from provider: {}", 
                        provider.getClass().getName(), e);
            }
        }
    }

    /**
     * 注册拒绝策略
     * 
     * @param policyName 策略名称
     * @param handler 拒绝策略处理器
     */
    public static void register(String policyName, RejectedExecutionHandler handler) {
        if (policyName == null || handler == null) {
            throw new IllegalArgumentException("Policy name and handler cannot be null");
        }
        POLICY_MAP.put(policyName, handler);
    }

    /**
     * 根据策略名称获取拒绝策略
     * 
     * @param policyName 策略名称
     * @return RejectedExecutionHandler 实例
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    public static RejectedExecutionHandler getPolicy(String policyName) {
        // 确保已初始化
        if (!initialized) {
            init();
        }

        RejectedExecutionHandler handler = POLICY_MAP.get(policyName);
        if (handler == null) {
            throw new IllegalArgumentException("No matching rejected policy found: " + policyName + 
                    ". Available policies: " + POLICY_MAP.keySet());
        }
        return handler;
    }

    /**
     * 检查策略是否存在
     * 
     * @param policyName 策略名称
     * @return true 表示存在，false 表示不存在
     */
    public static boolean hasPolicy(String policyName) {
        if (!initialized) {
            init();
        }
        return POLICY_MAP.containsKey(policyName);
    }

    /**
     * 获取所有已注册的策略名称
     * 
     * @return 策略名称集合
     */
    public static java.util.Set<String> getAllPolicyNames() {
        if (!initialized) {
            init();
        }
        return POLICY_MAP.keySet();
    }
}

