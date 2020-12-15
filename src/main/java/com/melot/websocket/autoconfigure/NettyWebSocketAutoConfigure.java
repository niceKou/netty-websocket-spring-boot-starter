package com.melot.websocket.autoconfigure;

import com.melot.websocket.annotation.EnableWebSocket;
import com.melot.websocket.netty.NettyWebSocketSelector;
import com.melot.websocket.spring.ServerEndpointExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

@EnableWebSocket
@ConditionalOnMissingBean(NettyWebSocketSelector.class)
public class NettyWebSocketAutoConfigure {
}
