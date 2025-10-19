package com.infinite.gateway.core.filter.route;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.common.pojo.RouteDefinition;
import com.infinite.gateway.core.context.GatewayContext;
import com.infinite.gateway.core.executor.BizExecutorManager;
import com.infinite.gateway.core.filter.Filter;
import com.infinite.gateway.core.filter.route.resilience.Resilience;
import com.infinite.gateway.core.helper.ResponseHelper;
import com.infinite.gateway.core.http.HttpClient;
import io.netty.util.concurrent.EventExecutorGroup;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
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
     *
     * 关键设计：
     * 1. AHC的响应回调切到业务线程池执行（继续执行后续过滤器链等业务逻辑）
     * 2. 最终的writeBackResponse会自动切回EventLoop执行IO写操作
     *
     * 这样保证了：业务逻辑在业务线程池，IO操作在EventLoop，线程边界清晰
     */
    private void handleResponseAsync(GatewayContext context, CompletionStage<Response> responseStage) {
        // 获取业务线程池（用于执行后续过滤器链等业务逻辑）
        EventExecutorGroup bizExecutor = BizExecutorManager.getInstance().getBizEventExecutorGroup();
        // 将EventExecutorGroup转换为Executor（取next()获取一个EventExecutor）
        Executor executor = bizExecutor.next();

        responseStage.whenCompleteAsync((response, throwable) -> {
            try {
                if (throwable != null) {
                    context.setThrowable(throwable);
                    throw new RuntimeException(throwable);
                }
                // 构建网关响应（业务逻辑，在业务线程执行）
                context.setResponse(ResponseHelper.buildGatewayResponse(response));
                // 继续执行过滤器链（业务逻辑，在业务线程执行）
                context.doFilter();
            } catch (Exception e) {
                context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
                // writeBackResponse内部会切回EventLoop执行IO写操作
                context.writeBackResponse();
            }
        }, executor).exceptionallyAsync(throwable -> {
            context.setResponse(ResponseHelper.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
            // writeBackResponse内部会切回EventLoop执行IO写操作
            context.writeBackResponse();
            return null;
        }, executor);
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
