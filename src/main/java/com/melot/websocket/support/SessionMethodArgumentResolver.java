package com.melot.websocket.support;

import com.melot.websocket.handler.EndpointServerHandler;
import com.melot.websocket.model.Session;
import io.netty.channel.Channel;
import org.springframework.core.MethodParameter;

public class SessionMethodArgumentResolver implements MethodArgumentResolver {

    /**
     * 方法该参数类型是 session类型的
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Session.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * @param parameter
     * @param channel
     * @param object
     * @return session
     * @throws Exception
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
        return channel.attr(EndpointServerHandler.SESSION_KEY).get();
    }
}
