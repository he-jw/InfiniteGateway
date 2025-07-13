package com.infinite.gateway.core.netty;

import com.infinite.gateway.common.util.SystemUtil;
import com.infinite.gateway.config.config.Config;
import com.infinite.gateway.config.config.HttpClientConfig;
import com.infinite.gateway.core.LifeCycle;
import com.infinite.gateway.core.http.HttpClient;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;

import java.io.IOException;

@Slf4j
public class NettyHttpClient implements LifeCycle {

    private final Config config;

    private final EventLoopGroup eventLoopGroupWorker;

    private AsyncHttpClient asyncHttpClient;

    public NettyHttpClient(Config config) {
        this.config = config;
        if (SystemUtil.isLinuxPlatform()) {
            eventLoopGroupWorker = new EpollEventLoopGroup(
                    config.getHttpClient().getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("epoll-netty-worker-nio"));
        } else {
            eventLoopGroupWorker = new NioEventLoopGroup(
                    config.getHttpClient().getEventLoopGroupWorkerNum(),
                    new DefaultThreadFactory("default-netty-worker-nio"));
        }
    }

    @Override
    public void start() {
        HttpClientConfig httpClientConfig = config.getHttpClient();
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                .setEventLoopGroup(eventLoopGroupWorker) // 使用传入的Netty事件循环组
                .setConnectTimeout(httpClientConfig.getHttpConnectTimeout()) // 连接超时设置
                .setRequestTimeout(httpClientConfig.getHttpRequestTimeout()) // 请求超时设置
                .setMaxRedirects(httpClientConfig.getHttpMaxRedirects()) // 最大重定向次数
                .setAllocator(PooledByteBufAllocator.DEFAULT) // 使用池化的ByteBuf分配器以提升性能
                .setCompressionEnforced(true) // 强制压缩
                .setMaxConnections(httpClientConfig.getHttpMaxConnections()) // 最大连接数
                .setMaxConnectionsPerHost(httpClientConfig.getHttpConnectionsPerHost()) // 每个主机的最大连接数
                .setPooledConnectionIdleTimeout(httpClientConfig.getHttpPooledConnectionIdleTimeout()); // 连接池中空闲连接的超时时间
        // 根据配置创建异步HTTP客户端
        this.asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
        HttpClient.getInstance().initialized(asyncHttpClient);
    }

    @Override
    public void shutdown() {
        if (this.asyncHttpClient != null) {
            try {
                this.asyncHttpClient.close();
            } catch (IOException e) {
                log.error("NettyHttpClient shutdown error", e);
            }
        }
    }
}
