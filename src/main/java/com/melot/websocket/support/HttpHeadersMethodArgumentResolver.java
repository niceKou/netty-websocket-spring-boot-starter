package com.melot.websocket.support;

import com.melot.websocket.annotation.OnOpen;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.core.MethodParameter;

public class HttpHeadersMethodArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getMethod().isAnnotationPresent(OnOpen.class) && HttpHeaders.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * @param parameter
     * @param channel
     * @param object
     * @return HttpHeaders headers
     * @throws Exception
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
        return ((WebSocketServerProtocolHandler.HandshakeComplete) object).requestHeaders();
    }
}
