package com.infinite.gateway.core.filter.route;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.Filter;
import com.infinite.gateway.core.helper.ResponseHelper;
import com.infinite.gateway.core.http.HttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletableFuture;

import static com.infinite.gateway.common.constant.FilterConstant.ROUTE_FILTER_NAME;
import static com.infinite.gateway.common.constant.FilterConstant.ROUTE_FILTER_ORDER;

public class RouterFilter implements Filter {
    @Override
    public void doPreFilter(GatewayContext context) {
        RouteDefinition.ResilienceConfig resilienceConfig = context.getRoute().getResilienceConfig();
        if (resilienceConfig != null && resilienceConfig.isEnabled()) {
            // TODO
        } else {
            Request request = context.getRequest().buildUrl();
            CompletableFuture<Response> future = HttpClient.getInstance().executeRequest(request);
            future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    context.setThrowable(throwable);
                    System.out.println(throwable.getMessage());
                    throw new RuntimeException();
                }
                context.setResponse(ResponseHelper.buildGatewayResponse(response));
                context.doFilter();
            }).exceptionally(throwable -> {
                context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
                context.writeBackResponse();
                return null;
            });
        }
    }

    @Override
    public void doPostFilter(GatewayContext context) {
        context.doFilter();
    }

    @Override
    public String mark() {
        return ROUTE_FILTER_NAME;
    }

    @Override
    public int getOrder() {
        return ROUTE_FILTER_ORDER;
    }
}
