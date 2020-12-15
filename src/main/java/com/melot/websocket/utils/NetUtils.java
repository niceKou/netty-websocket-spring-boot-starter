package com.melot.websocket.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;


public class NetUtils {
    private Logger logger = LoggerFactory.getLogger(NetUtils.class);

    /**
     * @param hostName
     * @return ip address or hostName if UnknownHostException
     */
    public static String getIpByHost(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            return hostName;
        }
    }

    public static String toAddressString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }
}
