package com.melot.websocket.netty;

import com.melot.websocket.handler.EndpointServerHandler;
import com.melot.websocket.model.ServerEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public abstract class AbstractServer {
    private Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    // closing closed means the process is being closed and close is finished
    private volatile boolean closing;
    private volatile boolean closed;


    protected ServerEndpointConfig config;
    protected EndpointServerHandler handler;
    protected InetSocketAddress bindAddress;

    public AbstractServer(ServerEndpointConfig config, EndpointServerHandler handler) {
        this.handler = handler;
        this.config = config;
        this.bindAddress = new InetSocketAddress(config.getHost(), config.getPort());
    }

    public void open() throws Exception {
        try {
            doOpen();
        } catch (Throwable t) {
            if (logger.isInfoEnabled()) {
                logger.info("Start websocket server bind " + this.bindAddress);
            }
            throw new Exception("Failed to bind websocket server, cause: " + t.getMessage(), t);
        }
    }

    protected void startClose() {
        this.closing = true;
    }

    protected void close() {
        if (logger.isInfoEnabled()) {
            logger.info("Close Websocket Server bind " + getBindAddress());
        }
        if (isClosed()) {
            return;
        }
        this.closed = true;
        doClose();
    }

    protected abstract void doOpen() throws Throwable;

    protected abstract void doClose();


    public boolean isClosed() {
        return closed;
    }

    public boolean isClosing() {
        return closing && !closed;
    }

    protected InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    public int getAccepts() {
        return config.getAccepts();
    }

    public int getHeartbeat() {
        return config.getHeartbeat();
    }

    public String getPath() {
        return config.getPath();
    }
}
