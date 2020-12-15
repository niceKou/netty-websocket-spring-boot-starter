package com.melot.websocket.handler;

import com.melot.websocket.model.MethodMapping;
import com.melot.websocket.model.ServerEndpointConfig;
import com.melot.websocket.model.Session;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class EndpointServerHandler {
    private static final Logger logger = LoggerFactory.getLogger(EndpointServerHandler.class);

    private static final AttributeKey<Object> IMPLEMENT_KEY = AttributeKey.valueOf("WEBSOCKET_IMPLEMENT");

    public static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("WEBSOCKET_SESSION");

    private final MethodMapping methodMapping;
    private final ServerEndpointConfig config;

    public EndpointServerHandler(MethodMapping methodMapping, ServerEndpointConfig config) {
        this.methodMapping = methodMapping;
        this.config = config;
    }

    public void doOnOpen(Channel channel, HandshakeComplete handshake) {
        Object implement = channel.attr(IMPLEMENT_KEY).get();
        if (implement == null) {
            try {
                implement = methodMapping.getEndpointInstance();// 实例化 @ServerEndpoint的注解类
                channel.attr(IMPLEMENT_KEY).set(implement);
            } catch (Exception e) {
                logger.error("EndpointServerHandler doOnOpen Instance @ServerEndpoint error: ", e);
                return;
            }
            Session session = new Session(channel);
            channel.attr(SESSION_KEY).set(session);
        }
        try {
            Method method = methodMapping.getOnOpen();
            Object[] args = methodMapping.getOnOpenArgs(channel, handshake);
            method.invoke(implement, args);
        } catch (Throwable t) {
            logger.error("EndpointServerHandler invoke onOpen Method error: ", t);
        }
    }

    public void doOnMessage(Channel channel, WebSocketFrame frame) {
        if (methodMapping.getOnMessage() == null) return;
        TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
        Object implement = channel.attr(IMPLEMENT_KEY).get();
        try {
            Method method = methodMapping.getOnMessage();
            Object[] args = methodMapping.getOnMessageArgs(channel, textFrame);
            method.invoke(implement, args);
        } catch (Throwable t) {
            logger.error("EndpointServerHandler invoke onMessage Method error: ", t);
        }
    }

    public void doOnBinary(Channel channel, WebSocketFrame frame) {
        if (methodMapping.getOnBinary() == null) return;
        BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) frame;
        Object implement = channel.attr(IMPLEMENT_KEY).get();
        try {
            Method method = methodMapping.getOnBinary();
            Object[] args = methodMapping.getOnBinaryArgs(channel, binaryWebSocketFrame);
            method.invoke(implement, args);
        } catch (Throwable t) {
            logger.error("EndpointServerHandler invoke onBinary Method error: ", t);
        }
    }

    public void doOnEvent(Channel channel, Object evt) {
        if (methodMapping.getOnEvent() == null) return;
        if (!channel.hasAttr(SESSION_KEY)) return;
        Object implement = channel.attr(IMPLEMENT_KEY).get();
        try {
            Method method = methodMapping.getOnEvent();
            Object[] args = methodMapping.getOnEventArgs(channel, evt);
            method.invoke(implement, args);
        } catch (Throwable t) {
            logger.error("EndpointServerHandler invoke onEvent Method error: ", t);
        }
    }

    public void doOnClose(Channel channel) {
        if (methodMapping.getOnClose() == null) return;
        if (!channel.hasAttr(SESSION_KEY)) return;
        Object implement = channel.attr(IMPLEMENT_KEY).get();
        try {
            Method method = methodMapping.getOnClose();
            Object[] args = methodMapping.getOnCloseArgs(channel);
            method.invoke(implement, args);
        } catch (Throwable t) {
            logger.error("EndpointServerHandler invoke onClose Method error: ", t);
        }
    }

    public void doOnError(Channel channel, Throwable throwable) {
        if (methodMapping.getOnError() == null) return;
        if (!channel.hasAttr(SESSION_KEY)) return;
        Object implement = channel.attr(IMPLEMENT_KEY).get();
        try {
            Method method = methodMapping.getOnError();
            Object[] args = methodMapping.getOnErrorArgs(channel, throwable);
            method.invoke(implement, args);
        } catch (Throwable t) {
            logger.error("EndpointServerHandler invoke onError Method error: ", t);
        }
    }

    public String getHost() {
        return config.getHost();
    }

    public int getPort() {
        return config.getPort();
    }

    public String getPath() {
        return config.getPath();
    }
}
