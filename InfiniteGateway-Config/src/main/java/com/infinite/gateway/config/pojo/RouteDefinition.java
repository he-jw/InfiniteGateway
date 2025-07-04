package com.infinite.gateway.config.pojo;

import com.infinite.gateway.common.enums.FlowEnum;
import lombok.Data;

import java.util.*;

import static com.infinite.gateway.common.constant.GrayConstant.MAX_GRAY_THRESHOLD;
import static com.infinite.gateway.common.constant.GrayConstant.THRESHOLD_GRAY_STRATEGY;
import static com.infinite.gateway.common.constant.LoadBalanceConstant.ROUND_ROBIN_LOAD_BALANCE_STRATEGY;
import static com.infinite.gateway.common.constant.LoadBalanceConstant.VIRTUAL_NODE_NUM;
import static com.infinite.gateway.common.enums.FlowEnum.TOKEN_BUCKET;

@Data
public class RouteDefinition implements Comparable<RouteDefinition>{

    /**
     * 路由ID,全局唯一
     */
    private String id = UUID.randomUUID().toString();

    /**
     * 后端服务ID
     */
    private String serviceId;

    /**
     * 路径集合
     */
    private List<String> paths;

    /**
     * 规则排序，对应场景：一个路径对应多条规则，然后只执行一条规则的情况, 路由的排序，值越小越靠前
     */
    private int order = 0;

    /**
     * 路由要走的过滤器
     */
    private Set<FilterConfig> filterConfigs = new HashSet<>();

    @Override
    public int compareTo(RouteDefinition o) {
        int orderCompare = Integer.compare(getOrder(), o.getOrder());
        if (orderCompare == 0) {
            return getId().compareTo(o.getId());
        }
        return orderCompare;
    }


    @Data
    public static class FilterConfig {

        /**
         * 过滤器名字，唯一的
         */
        private String name;

        /**
         * 是否启用过滤器
         */
        private boolean enable = true;

        /**
         * 过滤器规则描述，json
         */
        private String config;

    }

    @Data
    public static class GrayFilterConfig {

        /**
         * 灰度策略名，默认根据流量
         */
        private String strategyName = THRESHOLD_GRAY_STRATEGY;

        /**
         * 灰度流量最大比例
         */
        private double maxGrayThreshold = MAX_GRAY_THRESHOLD;


    }

    @Data
    public static class LoadBalanceFilterConfig {

        /**
         * 负载均衡策略名，默认轮询
         */
        private String strategyName = ROUND_ROBIN_LOAD_BALANCE_STRATEGY;

        /**
         * 是否开启严格轮询
         */
        private boolean isStrictRoundRobin = true;

        /**
         * 一致性哈希算法虚拟节点个数
         */
        private int virtualNodeNum = VIRTUAL_NODE_NUM;

    }

    @Data
    public static class FlowFilterConfig {

        /**
         * 是否开启流控
         */
        private boolean enabled = false;

        /**
         * 流控类型
         */
        private FlowEnum type = TOKEN_BUCKET;

        /**
         * 容量
         */
        private int capacity = 1000;

        /**
         * 速率
         * 如果是滑动窗口则是窗口大小，单位 ms
         * 如果是令牌桶，则是令牌桶生成速率，单位 个/s
         * 如果是漏桶，则是漏桶速率，单位 ms/个
         */
        private int rate = 500;

    }
}






















