package com.infinite.gateway.core.filter.route;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.filter.Filter;
import com.infinite.gateway.core.filter.route.resilience.Resilience;
import com.infinite.gateway.core.helper.ResponseHelper;
import com.infinite.gateway.core.http.HttpClient;
import io.netty.channel.EventLoop;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static com.infinite.gateway.common.constant.FilterConstant.ROUTE_FILTER_NAME;
import static com.infinite.gateway.common.constant.FilterConstant.ROUTE_FILTER_ORDER;

public class RouterFilter implements Filter {
    @Override
    public void doPreFilter(GatewayContext context) {
        RouteDefinition.ResilienceConfig resilienceConfig = context.getRoute().getResilienceConfig();
        if (resilienceConfig != null && resilienceConfig.isEnabled()) {
            resilienceRoute(context);
        } else {
            route(context);
        }
    }

    private void resilienceRoute(GatewayContext context) {
        Supplier<CompletionStage<Response>> supplier = buildRouteSupplier(context);
        CompletionStage<Response> responseStage = Resilience.getInstance().build(context, supplier).get();
        handleResponseAsync(context, responseStage);
    }

    private void route(GatewayContext context) {
        CompletionStage<Response> responseStage = buildRouteSupplier(context).get();
        handleResponseAsync(context, responseStage);
    }

    /**
     * 处理异步响应的公共方法
     * 将响应处理任务重新交给服务端Netty的EventLoop执行
     */
    private void handleResponseAsync(GatewayContext context, CompletionStage<Response> responseStage) {
        EventLoop serverEventLoop = context.getNettyCtx().channel().eventLoop();
        
        responseStage.whenCompleteAsync((response, throwable) -> {
            try {
                if (throwable != null) {
                    context.setThrowable(throwable);
                    throw new RuntimeException(throwable);
                }
                context.setResponse(ResponseHelper.buildGatewayResponse(response));
                context.doFilter();
            } catch (Exception e) {
                context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
                context.writeBackResponse();
            }
        }, serverEventLoop).exceptionallyAsync(throwable -> {
            context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
            context.writeBackResponse();
            return null;
        }, serverEventLoop);
    }

    private Supplier<CompletionStage<Response>> buildRouteSupplier(GatewayContext context) {
        Request request = context.getRequest().buildUrl();
        return () -> HttpClient.getInstance().executeRequest(request);
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
