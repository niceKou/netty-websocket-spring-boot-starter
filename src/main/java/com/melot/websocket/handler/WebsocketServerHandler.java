package com.melot.websocket.handler;

import com.melot.websocket.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;

@io.netty.channel.ChannelHandler.Sharable
public class WebsocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static Logger logger = LoggerFactory.getLogger(WebsocketServerHandler.class);

    private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>();
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    private final CopyOnWriteArrayList<Channel> UN_COMPLETE_HANDSHAKER_CHANNELS = new CopyOnWriteArrayList<>();
    private final AttributeKey<Long> ACTIVE_TIME_ATTR_KEY = AttributeKey.valueOf("activeTime");
    private int accepts;
    private EndpointServerHandler handler;

    public WebsocketServerHandler(EndpointServerHandler handler, int accepts) {
        this.handler = handler;
        this.accepts = accepts;
        startTimeoutTask();
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        // connection control
        if (accepts > 0 && channels.size() > accepts) {
            logger.error("Close channel " + channel.id() + ", cause: The server " + NetUtils.toAddressString((InetSocketAddress) channel.localAddress()) + " connections greater than max config " + accepts);
            channel.close();
        }
        channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), ctx.channel());
        ctx.channel().attr(ACTIVE_TIME_ATTR_KEY).set(System.currentTimeMillis());
        UN_COMPLETE_HANDSHAKER_CHANNELS.add(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            handler.doOnMessage(ctx.channel(), frame);
        } else if (frame instanceof BinaryWebSocketFrame) {
            handler.doOnBinary(ctx.channel(), frame);
        }
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Channel ch = ctx.channel();
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            UN_COMPLETE_HANDSHAKER_CHANNELS.remove(ch);
            handler.doOnOpen(ch, (WebSocketServerProtocolHandler.HandshakeComplete) evt);
        } else if (evt instanceof IdleStateEvent) {
            handler.doOnEvent(ch, evt);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        UN_COMPLETE_HANDSHAKER_CHANNELS.remove(ctx.channel());
        channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
        handler.doOnClose(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handler.doOnError(ctx.channel(), cause);
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    /**
     * Complete the websocket protocol handshake within 30s, otherwise disconnect
     */
    private void startTimeoutTask() {
        service.scheduleAtFixedRate(() -> {
            for (Channel channel : UN_COMPLETE_HANDSHAKER_CHANNELS) {
                long timestamp = channel.attr(ACTIVE_TIME_ATTR_KEY).get();
                if (System.currentTimeMillis() - timestamp > 30000) {
                    channel.close();
                }
            }
        }, 0, 10000, TimeUnit.MILLISECONDS);
    }
}
