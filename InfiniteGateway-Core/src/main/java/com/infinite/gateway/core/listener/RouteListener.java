package com.infinite.gateway.core.listener;

import com.infinite.gateway.common.pojo.RouteDefinition;

public interface RouteListener {

    void changeOnRoute(RouteDefinition routeDefinition);

}
