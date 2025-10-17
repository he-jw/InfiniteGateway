package com.infinite.gateway.core.filter.gary.strategy;

import com.infinite.gateway.common.pojo.ServiceInstance;
import com.infinite.gateway.core.context.GatewayContext;

import java.util.List;

public interface GrayStrategy {

     void execute(GatewayContext context, List<ServiceInstance> instances);

     String mark();

}
