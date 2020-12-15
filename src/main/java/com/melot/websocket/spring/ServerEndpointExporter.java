package com.melot.websocket.spring;

import com.melot.websocket.annotation.ServerEndpoint;
import com.melot.websocket.handler.EndpointServerHandler;
import com.melot.websocket.model.MethodMapping;
import com.melot.websocket.model.ServerEndpointConfig;
import com.melot.websocket.netty.NettyServer;
import com.melot.websocket.utils.NetUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ServerEndpointExporter extends ApplicationObjectSupport implements SmartInitializingSingleton, BeanFactoryAware {
    private final Map<InetSocketAddress, NettyServer> nettyServerMap = new HashMap<>();

    private AbstractBeanFactory beanFactory;

    @Override
    public void afterSingletonsInstantiated() {
        registerEndpoints();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof AbstractBeanFactory)) {
            throw new IllegalArgumentException("AutowiredAnnotationBeanPostProcessor requires a AbstractBeanFactory: " + beanFactory);
        }
        this.beanFactory = (AbstractBeanFactory) beanFactory;
    }

    private void registerEndpoints() {
        Set<Class<?>> endpointClasses = new LinkedHashSet<>();

        ApplicationContext context = getApplicationContext();
        if (context != null) {
            String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
            for (String beanName : endpointBeanNames) {
                endpointClasses.add(context.getType(beanName));
            }
        }

        for (Class<?> endpointClass : endpointClasses) {
            registerEndpoint(endpointClass);
        }

        init();
    }

    private void registerEndpoint(Class<?> endpointClass) {
        ServerEndpoint annotation = AnnotatedElementUtils.findMergedAnnotation(endpointClass, ServerEndpoint.class);
        if (annotation == null) {
            throw new IllegalStateException("missingAnnotation ServerEndpoint");
        }

        ApplicationContext context = getApplicationContext();
        MethodMapping methodMapping = null;
        try {
            methodMapping = new MethodMapping(endpointClass, context, beanFactory);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register ServerEndpointConfig: ", e);
        }

        ServerEndpointConfig serverEndpointConfig = getEndpointConfig(annotation);
        InetSocketAddress address = new InetSocketAddress(serverEndpointConfig.getHost(), serverEndpointConfig.getPort());
        NettyServer nettyServer = nettyServerMap.get(address);
        if (nettyServer != null) {
            throw new IllegalStateException("port already bean bind " + serverEndpointConfig.getPort());
        }
        EndpointServerHandler handler = new EndpointServerHandler(methodMapping, serverEndpointConfig);
        nettyServer = new NettyServer(serverEndpointConfig, handler);
        nettyServerMap.put(address, nettyServer);
    }


    private void init() {
        for (Map.Entry<InetSocketAddress, NettyServer> entry : nettyServerMap.entrySet()) {
            InetSocketAddress address = entry.getKey();
            NettyServer nettyServer = entry.getValue();
            try {
                nettyServer.open();
            } catch (Exception e) {
                logger.error("websocket netty server open " + NetUtils.toAddressString(address) + " error: ", e);
            }
        }
    }

    private ServerEndpointConfig getEndpointConfig(ServerEndpoint annotation) {
        ServerEndpointConfig serverEndpointConfig = new ServerEndpointConfig();
        serverEndpointConfig.setHost(resolveAnnotationValue(annotation.host(), String.class, "host"));
        serverEndpointConfig.setPort(resolveAnnotationValue(annotation.port(), Integer.class, "port"));
        serverEndpointConfig.setPath(resolveAnnotationValue(annotation.path(), String.class, "path"));
        serverEndpointConfig.setIothreads(resolveAnnotationValue(annotation.iothreads(), Integer.class, "iothreads"));
        serverEndpointConfig.setAccepts(resolveAnnotationValue(annotation.accepts(), Integer.class, "accept"));
        serverEndpointConfig.setHeartbeat(resolveAnnotationValue(annotation.heartbeat(), Integer.class, "heartbeat"));
        serverEndpointConfig.setReaderIdleTime(resolveAnnotationValue(annotation.readerIdleTime(), Integer.class, "readerIdleTime"));
        serverEndpointConfig.setWriteIdleTime(resolveAnnotationValue(annotation.writeIdleTime(), Integer.class, "writeIdleTime"));
        serverEndpointConfig.setAllIdleTime(resolveAnnotationValue(annotation.allIdleTime(), Integer.class, "allIdleTime"));
        serverEndpointConfig.setMaxContentLength(resolveAnnotationValue(annotation.maxContentLength(), Integer.class, "maxContentLength"));
        return serverEndpointConfig;
    }

    /**
     * 解析注解通配符
     */
    private <T> T resolveAnnotationValue(Object value, Class<T> requiredType, String paramName) {
        if (value == null) return null;
        TypeConverter typeConverter = beanFactory.getTypeConverter();
        if (value instanceof String) {
            String strVal = beanFactory.resolveEmbeddedValue((String) value);
            BeanExpressionResolver beanExpressionResolver = beanFactory.getBeanExpressionResolver();
            if (beanExpressionResolver != null) {
                value = beanExpressionResolver.evaluate(strVal, new BeanExpressionContext(beanFactory, null));
            } else {
                value = strVal;
            }
        }
        try {
            return typeConverter.convertIfNecessary(value, requiredType);
        } catch (TypeMismatchException e) {
            throw new IllegalArgumentException("Failed to convert value of parameter '" + paramName + "' to required type '" + requiredType.getName() + "'");
        }
    }
}
