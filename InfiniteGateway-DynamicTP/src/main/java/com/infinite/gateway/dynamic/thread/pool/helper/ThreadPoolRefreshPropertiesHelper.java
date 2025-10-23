package com.infinite.gateway.dynamic.thread.pool.helper;

import cn.hutool.core.collection.CollUtil;
import com.infinite.gateway.dynamic.thread.pool.bq.VariableLinkedBlockingQueue;
import com.infinite.gateway.dynamic.thread.pool.enums.BlockingQueueTypeEnum;
import com.infinite.gateway.dynamic.thread.pool.enums.RejectedPolicyTypeEnum;
import com.infinite.gateway.dynamic.thread.pool.holder.ThreadPoolExecutorHolder;
import com.infinite.gateway.dynamic.thread.pool.holder.ThreadPoolExecutorRegister;
import com.infinite.gateway.dynamic.thread.pool.properties.RemoteThreadPoolExecutorProperties;
import com.infinite.gateway.dynamic.thread.pool.properties.ThreadPoolExecutorProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.infinite.gateway.dynamic.thread.pool.constants.Constants.CHANGE_DELIMITER;
import static com.infinite.gateway.dynamic.thread.pool.constants.Constants.CHANGE_THREAD_POOL_TEXT;

@Slf4j
public class ThreadPoolRefreshPropertiesHelper {


    public static void refresherDynamicThreadPool(RemoteThreadPoolExecutorProperties refresherProperties) {
        if (CollUtil.isEmpty(refresherProperties.getExecutors())) {
            return;
        }
        for (ThreadPoolExecutorProperties remoteProperties : refresherProperties.getExecutors()) {
            String threadPoolId = remoteProperties.getThreadPoolId();
            synchronized (threadPoolId.intern()) {
                boolean changed = hasThreadPoolConfigChanged(remoteProperties);
                if (!changed) {
                    continue;
                }

                // 将远程配置应用到线程池，更新相关参数
                updateThreadPoolFromRemoteConfig(remoteProperties);

                // 线程池参数变更后进行日志打印
                ThreadPoolExecutorHolder holder = ThreadPoolExecutorRegister.getHolder(threadPoolId);
                ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();
                holder.setExecutorProperties(remoteProperties);

                // 打印线程池配置变更日志
                log.info(CHANGE_THREAD_POOL_TEXT,
                        threadPoolId,
                        String.format(CHANGE_DELIMITER, originalProperties.getCorePoolSize(), remoteProperties.getCorePoolSize()),
                        String.format(CHANGE_DELIMITER, originalProperties.getMaximumPoolSize(), remoteProperties.getMaximumPoolSize()),
                        String.format(CHANGE_DELIMITER, originalProperties.getQueueCapacity(), remoteProperties.getQueueCapacity()),
                        String.format(CHANGE_DELIMITER, originalProperties.getKeepAliveTime(), remoteProperties.getKeepAliveTime()),
                        String.format(CHANGE_DELIMITER, originalProperties.getRejectedHandler(), remoteProperties.getRejectedHandler()),
                        String.format(CHANGE_DELIMITER, originalProperties.getAllowCoreThreadTimeOut(), remoteProperties.getAllowCoreThreadTimeOut())
                );
            }
        }
    }


    public static boolean hasThreadPoolConfigChanged(ThreadPoolExecutorProperties remoteProperties) {
        String threadPoolId = remoteProperties.getThreadPoolId();
        ThreadPoolExecutorHolder holder = ThreadPoolExecutorRegister.getHolder(threadPoolId);
        if (holder == null) {
            log.warn("No thread pool found for thread pool id: {}", threadPoolId);
            return false;
        }
        ThreadPoolExecutor executor = holder.getExecutor();
        ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();

        return hasDifference(originalProperties, remoteProperties, executor);
    }

    public static void updateThreadPoolFromRemoteConfig(ThreadPoolExecutorProperties remoteProperties) {
        String threadPoolId = remoteProperties.getThreadPoolId();
        ThreadPoolExecutorHolder holder = ThreadPoolExecutorRegister.getHolder(threadPoolId);
        ThreadPoolExecutor executor = holder.getExecutor();
        ThreadPoolExecutorProperties originalProperties = holder.getExecutorProperties();

        Integer remoteCorePoolSize = remoteProperties.getCorePoolSize();
        Integer remoteMaximumPoolSize = remoteProperties.getMaximumPoolSize();
        if (remoteCorePoolSize != null && remoteMaximumPoolSize != null) {
            int originalMaximumPoolSize = executor.getMaximumPoolSize();
            if (remoteCorePoolSize > originalMaximumPoolSize) {
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
                executor.setCorePoolSize(remoteCorePoolSize);
            } else {
                executor.setCorePoolSize(remoteCorePoolSize);
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
            }
        } else {
            if (remoteMaximumPoolSize != null) {
                executor.setMaximumPoolSize(remoteMaximumPoolSize);
            }
            if (remoteCorePoolSize != null) {
                executor.setCorePoolSize(remoteCorePoolSize);
            }
        }

        if (remoteProperties.getAllowCoreThreadTimeOut() != null &&
                !Objects.equals(remoteProperties.getAllowCoreThreadTimeOut(), originalProperties.getAllowCoreThreadTimeOut())) {
            executor.allowCoreThreadTimeOut(remoteProperties.getAllowCoreThreadTimeOut());
        }

        if (remoteProperties.getRejectedHandler() != null &&
                !Objects.equals(remoteProperties.getRejectedHandler(), originalProperties.getRejectedHandler())) {
            RejectedExecutionHandler handler = RejectedPolicyTypeEnum.createPolicy(remoteProperties.getRejectedHandler());
            executor.setRejectedExecutionHandler(handler);
        }

        if (remoteProperties.getKeepAliveTime() != null &&
                !Objects.equals(remoteProperties.getKeepAliveTime(), originalProperties.getKeepAliveTime())) {
            executor.setKeepAliveTime(remoteProperties.getKeepAliveTime(), TimeUnit.SECONDS);
        }

        if (isQueueCapacityChanged(originalProperties, remoteProperties, executor)) {
            BlockingQueue<Runnable> queue = executor.getQueue();
            VariableLinkedBlockingQueue<?> resizableQueue = (VariableLinkedBlockingQueue<?>) queue;
            resizableQueue.setCapacity(remoteProperties.getQueueCapacity());
        }
    }

    public static boolean hasDifference(ThreadPoolExecutorProperties originalProperties,
                                  ThreadPoolExecutorProperties remoteProperties,
                                  ThreadPoolExecutor executor) {
        return isChanged(originalProperties.getCorePoolSize(), remoteProperties.getCorePoolSize())
                || isChanged(originalProperties.getMaximumPoolSize(), remoteProperties.getMaximumPoolSize())
                || isChanged(originalProperties.getAllowCoreThreadTimeOut(), remoteProperties.getAllowCoreThreadTimeOut())
                || isChanged(originalProperties.getKeepAliveTime(), remoteProperties.getKeepAliveTime())
                || isChanged(originalProperties.getRejectedHandler(), remoteProperties.getRejectedHandler())
                || isQueueCapacityChanged(originalProperties, remoteProperties, executor);
    }

    public static  <T> boolean isChanged(T before, T after) {
        return after != null && !Objects.equals(before, after);
    }

    public static boolean isQueueCapacityChanged(ThreadPoolExecutorProperties originalProperties,
                                           ThreadPoolExecutorProperties remoteProperties,
                                           ThreadPoolExecutor executor) {
        Integer remoteCapacity = remoteProperties.getQueueCapacity();
        Integer originalCapacity = originalProperties.getQueueCapacity();
        BlockingQueue<?> queue = executor.getQueue();

        return remoteCapacity != null
                && !Objects.equals(remoteCapacity, originalCapacity)
                && Objects.equals(BlockingQueueTypeEnum.VARIABLE_LINKED_BLOCKING_QUEUE.getName(), queue.getClass().getSimpleName());
    }

}
