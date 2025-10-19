package com.infinite.gateway.core.filter.gary.strategy;

import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;

import java.util.List;

import static com.infinite.gateway.common.constant.GrayConstant.CLIENT_IP_GRAY_STRATEGY;
import static com.infinite.gateway.common.constant.GrayConstant.MAX_GRAY_THRESHOLD;

public class ClientIPGrayStrategy implements GrayStrategy {

    @Override
    public void execute(GatewayContext context, List<ServiceInstance> instances) {
        double threshold = instances.stream().mapToDouble(ServiceInstance::getThreshold).sum();
        threshold = Math.min(threshold, MAX_GRAY_THRESHOLD);
        boolean isGray = context.getRequest().getClientIp().hashCode() % 100 < threshold * 100;
        context.getRequest().setGray(isGray);
    }

    @Override
    public String mark() {
        return CLIENT_IP_GRAY_STRATEGY;
    }
}
