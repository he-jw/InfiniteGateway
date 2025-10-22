package com.infinite.gateway.dynamic.thread.pool.enums;

import com.infinite.gateway.dynamic.thread.pool.registry.RejectedPolicyRegistry;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 拒绝策略类型枚举
 *
 * <p>注意：此枚举已废弃直接使用，推荐使用 {@link RejectedPolicyRegistry} 获取策略</p>
 * <p>保留此枚举是为了向后兼容</p>
 */
@Getter
public enum RejectedPolicyTypeEnum {

    /**
     * {@link ThreadPoolExecutor.CallerRunsPolicy}
     */
    CALLER_RUNS_POLICY("CallerRunsPolicy", new ThreadPoolExecutor.CallerRunsPolicy()),

    /**
     * {@link ThreadPoolExecutor.AbortPolicy}
     */
    ABORT_POLICY("AbortPolicy", new ThreadPoolExecutor.AbortPolicy()),

    /**
     * {@link ThreadPoolExecutor.DiscardPolicy}
     */
    DISCARD_POLICY("DiscardPolicy", new ThreadPoolExecutor.DiscardPolicy()),

    /**
     * {@link ThreadPoolExecutor.DiscardOldestPolicy}
     */
    DISCARD_OLDEST_POLICY("DiscardOldestPolicy", new ThreadPoolExecutor.DiscardOldestPolicy());

    private String name;

    private RejectedExecutionHandler rejectedHandler;

    RejectedPolicyTypeEnum(String rejectedPolicyName, RejectedExecutionHandler rejectedHandler) {
        this.name = rejectedPolicyName;
        this.rejectedHandler = rejectedHandler;
    }

    private static final Map<String, RejectedPolicyTypeEnum> NAME_TO_ENUM_MAP;

    static {
        final RejectedPolicyTypeEnum[] values = RejectedPolicyTypeEnum.values();
        NAME_TO_ENUM_MAP = new HashMap<>(values.length);
        for (RejectedPolicyTypeEnum value : values) {
            NAME_TO_ENUM_MAP.put(value.name, value);
        }
    }

    /**
     * Creates a {@link RejectedExecutionHandler} based on the given policy name.
     *
     * <p>此方法会优先从 {@link RejectedPolicyRegistry} 查找策略（支持 SPI 扩展），
     * 如果找不到则从枚举中查找（向后兼容）</p>
     *
     * @param rejectedPolicyName the policy name
     * @return the corresponding {@link RejectedExecutionHandler} instance
     * @throws IllegalArgumentException if no matching rejected policy type is found
     */
    public static RejectedExecutionHandler createPolicy(String rejectedPolicyName) {
        // 优先从注册表获取（支持 SPI 扩展）
        if (RejectedPolicyRegistry.hasPolicy(rejectedPolicyName)) {
            return RejectedPolicyRegistry.getPolicy(rejectedPolicyName);
        }

        // 向后兼容：从枚举中查找
        RejectedPolicyTypeEnum rejectedPolicyTypeEnum = NAME_TO_ENUM_MAP.get(rejectedPolicyName);
        if (rejectedPolicyTypeEnum != null) {
            return rejectedPolicyTypeEnum.rejectedHandler;
        }

        throw new IllegalArgumentException("No matching type of rejected execution was found: " + rejectedPolicyName);
    }
}