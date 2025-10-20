package com.infinite.gateway.core.netty.http2;

import com.infinite.gateway.core.netty.processor.NettyProcessor;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP/2 升级编解码工厂（单例）
 *
 * 职责：
 * 1. 判断是否为 h2c 协议升级请求
 * 2. 若是，创建 Http2FrameCodec + Http2MultiplexHandler 并返回升级编解码器
 * 3. 若否，返回 null（保持 HTTP/1.1）
 *
 * 为什么是单例：
 * - UpgradeCodecFactory 是无状态的纯函数式接口
 * - 每次调用都创建新的 Http2FrameCodec 和 Http2MultiplexHandler 实例
 * - 多个 Channel 可以安全地共享同一个 factory 实例
 *
 * 注意：
 * - HttpServerCodec 仍需每个 Channel 新建（有状态的编解码器）
 * - Http2FrameCodec、Http2MultiplexHandler 由 factory 为每个升级连接新建
 */
@Slf4j
public class Http2UpgradeCodecFactory implements HttpServerUpgradeHandler.UpgradeCodecFactory {

    private final int maxContentLength;
    private final EventExecutorGroup bizEventExecutorGroup;
    private final NettyProcessor nettyProcessor;

    /**
     * 单例实例
     */
    private static Http2UpgradeCodecFactory instance;

    private Http2UpgradeCodecFactory(int maxContentLength,
                                     EventExecutorGroup bizEventExecutorGroup,
                                     NettyProcessor nettyProcessor) {
        this.maxContentLength = maxContentLength;
        this.bizEventExecutorGroup = bizEventExecutorGroup;
        this.nettyProcessor = nettyProcessor;
    }

    /**
     * 获取单例实例（双检锁）
     */
    public static Http2UpgradeCodecFactory getInstance(int maxContentLength,
                                                       EventExecutorGroup bizEventExecutorGroup,
                                                       NettyProcessor nettyProcessor) {
        if (instance == null) {
            synchronized (Http2UpgradeCodecFactory.class) {
                if (instance == null) {
                    instance = new Http2UpgradeCodecFactory(maxContentLength, bizEventExecutorGroup, nettyProcessor);
                    log.info("Http2UpgradeCodecFactory singleton initialized");
                }
            }
        }
        return instance;
    }

    @Override
    public HttpServerUpgradeHandler.UpgradeCodec newUpgradeCodec(CharSequence protocol) {
        if (Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME.equals(protocol)) {
            // 为本次升级创建新的 Http2FrameCodec（父通道编解码器）
            Http2FrameCodec http2FrameCodec = Http2FrameCodecBuilder.forServer().build();
            // 为本次升级创建新的 Http2MultiplexHandler（多路复用处理器）
            // 每个 HTTP/2 stream 会在子 Channel 上由 H2ChildChannelInitializer 初始化
            Http2MultiplexHandler multiplexHandler = new Http2MultiplexHandler(
                    new H2ChildChannelInitializer(
                            maxContentLength,
                            bizEventExecutorGroup,
                            nettyProcessor));
            // 返回升级编解码器
            return new Http2ServerUpgradeCodec(http2FrameCodec, multiplexHandler);
        }
        return null;
    }
}

