package com.infinite.gateway.core.helper;

import com.alibaba.nacos.common.utils.StringUtils;
import com.infinite.gateway.common.pojo.ServiceDefinition;
import com.infinite.gateway.core.request.GatewayRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.asynchttpclient.Request;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.infinite.gateway.common.constant.HttpConstant.HTTP_FORWARD_SEPARATOR;

/**
 * Netty服务端、网关、Http客户端之间的请求转换
 */
public class RequestHelper {

    public static GatewayRequest buildGatewayRequest(ServiceDefinition serviceDefinition, FullHttpRequest fullHttpRequest, ChannelHandlerContext ctx) {
        HttpHeaders headers = fullHttpRequest.headers();
        String host = headers.get(HttpHeaderNames.HOST);
        HttpMethod method = fullHttpRequest.getMethod();
        String uri = fullHttpRequest.getUri();
        String contentType = HttpUtil.getMimeType(fullHttpRequest) == null ? null :
                HttpUtil.getMimeType(fullHttpRequest).toString();
        Charset charset = HttpUtil.getCharset(fullHttpRequest, StandardCharsets.UTF_8);
        String clientIp = getClientIp(ctx, fullHttpRequest);

        return new GatewayRequest(
                serviceDefinition,
                charset,
                clientIp,
                host,
                uri,
                method,
                contentType,
                headers,
                fullHttpRequest);
    }

    public static Request buildHttpClientRequest(GatewayRequest gatewayRequest) {
        return gatewayRequest.buildUrl();
    }

    // TODO
    private static String getClientIp(ChannelHandlerContext ctx, FullHttpRequest request) {
        String xForwardedValue = request.headers().get(HTTP_FORWARD_SEPARATOR);
        String clientIp = null;
        if (StringUtils.isNotEmpty(xForwardedValue)) {
            List<String> values = Arrays.asList(xForwardedValue.split(", "));
            if (values.size() >= 1 && StringUtils.isNotBlank(values.get(0))) {
                clientIp = values.get(0);
            }
        }
        if (clientIp == null) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            clientIp = inetSocketAddress.getAddress().getHostAddress();
        }
        return clientIp;
    }

}
