package com.melot.websocket.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ServerEndpointConfig {
    private String host;
    private int port;
    private String path;
    private int iothreads;
    private int accepts;
    private int heartbeat;
    private int maxContentLength;
    private int readerIdleTime;
    private int writeIdleTime;
    private int allIdleTime;
}
