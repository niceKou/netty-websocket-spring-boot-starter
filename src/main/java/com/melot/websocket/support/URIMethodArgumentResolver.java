package com.melot.websocket.support;

import com.melot.websocket.annotation.URI;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.core.MethodParameter;

public class URIMethodArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(URI.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
        return  ((WebSocketServerProtocolHandler.HandshakeComplete) object).requestUri();
    }
}
