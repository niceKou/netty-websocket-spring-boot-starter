package com.melot.websocket.netty;

import com.melot.websocket.handler.EndpointServerHandler;
import com.melot.websocket.handler.WebsocketServerHandler;
import com.melot.websocket.model.ServerEndpointConfig;
import com.melot.websocket.utils.NetUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NettyServer extends AbstractServer {
    private Logger logger = LoggerFactory.getLogger(NettyServer.class);
    /**
     * work threads 默认cpu+1
     */
    private final static int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    /**
     * netty server bootstrap.
     */
    private ServerBootstrap bootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel channel;
    private Map<String, Channel> channels;


    public NettyServer(ServerEndpointConfig config, EndpointServerHandler handler) {
        super(config, handler);
    }

    protected void doOpen() throws Throwable {

        int ioThreads = config.getIothreads() <= 0 ? DEFAULT_IO_THREADS : config.getIothreads();
        bootstrap = new ServerBootstrap();
        bossGroup = NettyEventLoopFactory.eventLoopGroup(1, "NettyServerBoss");
        workerGroup = NettyEventLoopFactory.eventLoopGroup(ioThreads, "NettyServerWork");

        final WebsocketServerHandler websocketHandler = new WebsocketServerHandler(handler, config.getAccepts());
        channels = websocketHandler.getChannels();

        bootstrap.group(bossGroup, workerGroup)
                .channel(NettyEventLoopFactory.serverSocketChannelClass())
                .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        logger.info("on connected " + ch.localAddress());
                        ChannelPipeline pipeline = ch.pipeline();
                        //websocket协议本身是基于http协议的，所以这边也要使用http解编码器
                        pipeline.addLast("server-idle-handler", new IdleStateHandler(config.getReaderIdleTime(), config.getWriteIdleTime(), config.getAllIdleTime(), TimeUnit.SECONDS));
                        pipeline.addLast(new HttpServerCodec());
                        //为了处理大文件传输的情形，以块的方式来写的处理器
                        pipeline.addLast(new ChunkedWriteHandler());
                        //netty是基于分段请求的，它负责把多个HttpMessage组装成一个完整的Http请求或者响应
                        pipeline.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
                        // ws://server:port/path
                        pipeline.addLast(new WebSocketServerProtocolHandler(getPath()));
                        pipeline.addLast(websocketHandler);
                    }
                });
        // 绑定端口，开始接收进来的连接
        ChannelFuture channelFuture = bootstrap.bind(getBindAddress()).sync();
        logger.info(NettyServer.class + " 启动正在监听： " + channelFuture.channel().localAddress());
        // 等待服务器  socket 关闭
        channelFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }).sync();
        channel = channelFuture.channel();
    }

    private Collection<Channel> getChannels() {
        Collection<Channel> chs = new HashSet<Channel>();
        for (Channel channel : this.channels.values()) {
            if (channel.isActive()) {
                chs.add(channel);
            } else {
                channels.remove(NetUtils.toAddressString((InetSocketAddress) channel.remoteAddress()));
            }
        }
        return chs;
    }

    protected void doClose() {
        try {
            if (channel != null) channel.close();
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }

        Collection<Channel> channels = getChannels();
        if (channels != null && channels.size() > 0) {
            for (Channel channel : channels) {
                try {
                    channel.close();
                } catch (Throwable e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }

        try {
            if (channels != null) {
                channels.clear();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }
}
