package com.infinite.gateway.core.executor;

import com.infinite.gateway.common.enums.ResponseCode;
import com.infinite.gateway.core.helper.ResponseHelper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网关业务线程池拒绝策略处理器
 * 当业务线程池饱和时（队列满且所有线程都在工作），说明系统已经过载。
 * 此时应该立即拒绝新请求并返回友好的错误信息，而不是让请求堆积或超时。
 * 503 Service Unavailable：表示服务暂时不可用，建议客户端稍后重试。
 *
 * 实现策略：
 * 1. 通过反射从被拒绝的 Netty 内部任务中提取 ChannelHandlerContext 和 FullHttpRequest
 * 2. 如果提取成功，直接在 IO 线程构建并写回 503 响应
 * 3. 如果提取失败，仅记录日志（客户端会超时，但这是背压保护的必要代价）
 */
@Slf4j
public class GatewayRejectedExecutionHandler implements RejectedExecutionHandler {

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
    public void rejected(Runnable task, SingleThreadEventExecutor executor) {
        // 1. 增加拒绝计数
        long count = rejectedCount.incrementAndGet();

        // 2. 限流日志（避免日志刷屏）
        long now = System.currentTimeMillis();
        if (now - lastLogTime > LOG_INTERVAL_MS) {
            log.warn("[BackPressure] 业务线程池已饱和，拒绝任务执行。累计拒绝次数: {}, 线程池状态: {}",
                    count, getExecutorStatus(executor));
            lastLogTime = now;
        }

        // 3. 通过反射按类型匹配提取 ChannelHandlerContext 和 FullHttpRequest
        ChannelHandlerContext ctx = extractByType(task, ChannelHandlerContext.class);
        FullHttpRequest request = extractByType(task, FullHttpRequest.class);

        if (ctx != null && request != null) {
            // 成功提取到 ctx 和 request，直接在 IO 线程写回 503 响应
            handleRejectionWithResponse(ctx, request);
        } else {
            // 无法提取 ctx 或 request，只能记录日志
            // 这种情况下，客户端会因为没有响应而超时
            // 但这比让请求堆积导致系统崩溃要好
            log.error("[BackPressure] 无法从任务中提取 ChannelHandlerContext 或 FullHttpRequest，任务类型: {}。" +
                    "客户端可能会超时，但这是背压保护的必要代价。", task.getClass().getName());
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
     * 方法1：按类型匹配提取单个对象
     *
     * 通过遍历 task 对象的所有字段，按类型匹配找到第一个符合条件的对象。
     * 这种方法不依赖字段名，而是依赖字段的类型，具有更强的通用性和自适应能力。
     *
     * 工作流程：
     * 1. 检查 task 是否来自 Netty（包名以 io.netty 开头）
     * 2. 遍历 task 及其父类的所有声明字段
     * 3. 对每个字段进行类型检查，找到第一个匹配的对象
     * 4. 返回找到的对象，或 null
     *
     * @param task 被拒绝的 Runnable（通常是 Netty 内部任务）
     * @param targetType 目标类型
     * @param <T> 泛型类型
     * @return 提取的对象或 null
     */
    @SuppressWarnings("unchecked")
    private <T> T extractByType(Object task, Class<T> targetType) {
        if (task == null || targetType == null) {
            return null;
        }

        try {
            String taskClassName = task.getClass().getName();

            // 只处理 Netty 内部任务（包名以 io.netty 开头）
            if (!taskClassName.startsWith("io.netty")) {
                log.debug("[ReflectionExtractor] 任务不是 Netty 内部任务，跳过提取: {}", taskClassName);
                return null;
            }

            // 遍历 task 及其父类的所有字段
            Class<?> currentClass = task.getClass();
            while (currentClass != null && currentClass != Object.class) {
                Field[] declaredFields = currentClass.getDeclaredFields();

                for (Field field : declaredFields) {
                    // 检查字段类型是否匹配
                    if (targetType.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(task);

                            // 类型检查和转换
                            if (value != null && targetType.isInstance(value)) {
                                log.debug("[ReflectionExtractor] 成功提取 {}，字段名: {}",
                                    targetType.getSimpleName(), field.getName());
                                return (T) value;
                            }
                        } catch (IllegalAccessException e) {
                            log.debug("[ReflectionExtractor] 无法访问字段: {}", field.getName(), e);
                        }
                    }
                }

                // 继续查找父类
                currentClass = currentClass.getSuperclass();
            }

            log.debug("[ReflectionExtractor] 无法从任务中提取 {}，任务类型: {}",
                targetType.getSimpleName(), taskClassName);
            return null;

        } catch (Exception e) {
            log.debug("[ReflectionExtractor] 反射提取 {} 失败", targetType.getSimpleName(), e);
            return null;
        }
    }

    /**
     * 方法2：按类型匹配提取所有对象
     *
     * 通过遍历 task 对象的所有字段，按类型匹配找到所有符合条件的对象。
     * 这种方法适用于需要提取多个同类型对象的场景。
     *
     * 工作流程：
     * 1. 检查 task 是否来自 Netty（包名以 io.netty 开头）
     * 2. 遍历 task 及其父类的所有声明字段
     * 3. 对每个字段进行类型检查，收集所有匹配的对象
     * 4. 返回匹配对象列表
     *
     * @param task 被拒绝的 Runnable（通常是 Netty 内部任务）
     * @param targetType 目标类型
     * @param <T> 泛型类型
     * @return 提取的对象列表（可能为空）
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> extractAllByType(Object task, Class<T> targetType) {
        List<T> results = new ArrayList<>();

        if (task == null || targetType == null) {
            return results;
        }

        try {
            String taskClassName = task.getClass().getName();

            // 只处理 Netty 内部任务（包名以 io.netty 开头）
            if (!taskClassName.startsWith("io.netty")) {
                log.debug("[ReflectionExtractor] 任务不是 Netty 内部任务，跳过提取: {}", taskClassName);
                return results;
            }

            // 遍历 task 及其父类的所有字段
            Class<?> currentClass = task.getClass();
            while (currentClass != null && currentClass != Object.class) {
                Field[] declaredFields = currentClass.getDeclaredFields();

                for (Field field : declaredFields) {
                    // 检查字段类型是否匹配
                    if (targetType.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true);
                            Object value = field.get(task);

                            // 类型检查和转换
                            if (value != null && targetType.isInstance(value)) {
                                results.add((T) value);
                                log.debug("[ReflectionExtractor] 提取到 {}，字段名: {}",
                                    targetType.getSimpleName(), field.getName());
                            }
                        } catch (IllegalAccessException e) {
                            log.debug("[ReflectionExtractor] 无法访问字段: {}", field.getName(), e);
                        }
                    }
                }

                // 继续查找父类
                currentClass = currentClass.getSuperclass();
            }

            if (results.isEmpty()) {
                log.debug("[ReflectionExtractor] 无法从任务中提取任何 {}，任务类型: {}",
                    targetType.getSimpleName(), taskClassName);
            }

            return results;

        } catch (Exception e) {
            log.debug("[ReflectionExtractor] 反射提取 {} 失败", targetType.getSimpleName(), e);
            return results;
        }
    }

    /**
     * 获取执行器状态信息（用于日志）
     */
    private String getExecutorStatus(SingleThreadEventExecutor executor) {
        if (executor == null) {
            return "unknown";
        }
        return String.format("pendingTasks=%d, isShutdown=%s",
            executor.pendingTasks(), executor.isShutdown());
    }
}

