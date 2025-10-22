package com.infinite.gateway.core.executor;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.core.helper.ResponseHelper;
import com.infinite.gateway.core.netty.context.IoThreadRequestContextHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网关业务线程池拒绝策略处理器（JDK ThreadPoolExecutor 版本）
 * 当业务线程池饱和时（队列满且所有线程都在工作），说明系统已经过载。
 * 此时应该立即拒绝新请求并返回友好的错误信息，而不是让请求堆积或超时。
 * 503 Service Unavailable：表示服务暂时不可用，建议客户端稍后重试。
 */
@Slf4j
public class GatewayJdkRejectedExecutionHandler implements RejectedExecutionHandler {

    /**
     * 拒绝次数统计（用于监控和告警）
     * 使用AtomicLong保证线程安全且性能高
     */
    private final AtomicLong rejectedCount = new AtomicLong(0);

    /**
     * 上次日志打印时间（避免日志刷屏）
     */
    private volatile long lastLogTime = 0;

    /**
     * 日志打印间隔（毫秒），默认5秒打印一次
     */
    private static final long LOG_INTERVAL_MS = 5000L;

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 1. 增加拒绝计数
        long count = rejectedCount.incrementAndGet();

        // 2. 限流日志（避免日志刷屏）
        long now = System.currentTimeMillis();
        if (now - lastLogTime > LOG_INTERVAL_MS) {
            log.warn("[BackPressure] 业务线程池已饱和，拒绝任务执行。累计拒绝次数: {}, 线程池状态: {}",
                    count, getExecutorStatus(executor));
            lastLogTime = now;
        }

        // 3. 从 ThreadLocal 获取请求上下文
        IoThreadRequestContextHolder.RequestContext requestContext = IoThreadRequestContextHolder.get();
        ChannelHandlerContext ctx = requestContext.getCtx();
        FullHttpRequest request = requestContext.getRequest();

        if (ctx != null && request != null) {
            // 成功提取到 ctx 和 request，直接在 IO 线程写回 503 响应
            handleRejectionWithResponse(ctx, request);
        } else {
            // 无法提取 ctx 或 request，只能记录日志
            // 这种情况下，客户端会因为没有响应而超时
            // 但这比让请求堆积导致系统崩溃要好
            log.error("[BackPressure] 无法从任务中提取 ChannelHandlerContext 或 FullHttpRequest，任务类型: {}。" +
                    "客户端可能会超时，但这是背压保护的必要代价。", r.getClass().getName());
        }
    }

    /**
     * 处理拒绝：直接构建并写回 503 错误给客户端
     *
     * 注意：此方法在 IO 线程执行（EventLoop 线程），可以直接进行 Channel 写操作
     *
     * @param ctx ChannelHandlerContext
     * @param request FullHttpRequest
     */
    private void handleRejectionWithResponse(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            // 构建 503 Service Unavailable 响应
            FullHttpResponse response = ResponseHelper.buildHttpResponse(ResponseCode.SERVICE_UNAVAILABLE);

            // 判断是否需要保持连接
            boolean keepAlive = HttpUtil.isKeepAlive(request);

            // 写回响应
            if (keepAlive) {
                // 保持连接
                ctx.writeAndFlush(response);
            } else {
                // 关闭连接
                ctx.writeAndFlush(response).addListener(future -> ctx.close());
            }

            log.debug("[BackPressure] 成功写回 503 响应，keepAlive: {}", keepAlive);

        } catch (Exception e) {
            log.error("[BackPressure] 处理拒绝任务时发生异常", e);
        } finally {
            // 释放请求资源
            if (ReferenceCountUtil.refCnt(request) > 0) {
                ReferenceCountUtil.release(request);
            }
        }
    }

    /**
     * 获取执行器状态信息（用于日志）
     */
    private String getExecutorStatus(ThreadPoolExecutor executor) {
        if (executor == null) {
            return "unknown";
        }
        return String.format("activeCount=%d, poolSize=%d, queueSize=%d, isShutdown=%s",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size(),
                executor.isShutdown());
    }

    /**
     * 获取拒绝次数（用于监控）
     */
    public long getRejectedCount() {
        return rejectedCount.get();
    }
}

