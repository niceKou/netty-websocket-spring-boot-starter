package com.melot.websocket.model;

import com.melot.websocket.annotation.*;
import com.melot.websocket.support.*;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class MethodMapping {
    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final Method onOpen;
    private final Method onClose;
    private final Method onError;
    private final Method onMessage;
    private final Method onBinary;
    private final Method onEvent;

    private final MethodParameter[] onOpenParameters;
    private final MethodParameter[] onCloseParameters;
    private final MethodParameter[] onErrorParameters;
    private final MethodParameter[] onMessageParameters;
    private final MethodParameter[] onBinaryParameters;
    private final MethodParameter[] onEventParameters;

    private final MethodArgumentResolver[] onOpenArgResolvers;
    private final MethodArgumentResolver[] onMessageArgResolvers;
    private final MethodArgumentResolver[] onBinaryArgResolvers;
    private final MethodArgumentResolver[] onEventArgResolvers;
    private final MethodArgumentResolver[] onCloseArgResolvers;
    private final MethodArgumentResolver[] onErrorArgResolvers;

    private final Class clazz;
    private final ApplicationContext applicationContext;
    private final AbstractBeanFactory beanFactory;

    public MethodMapping(Class<?> clazz, ApplicationContext context, AbstractBeanFactory beanFactory) throws Exception {
        this.applicationContext = context;
        this.clazz = clazz;
        this.beanFactory = beanFactory;

        Method open = null;
        Method message = null;
        Method binary = null;
        Method event = null;
        Method close = null;
        Method error = null;

        Method[] clazzMethods = null;
        Class<?> currentClazz = clazz;
        while (!currentClazz.equals(Object.class)) {
            Method[] currentClazzMethods = currentClazz.getDeclaredMethods();
            if (currentClazz == clazz) {
                clazzMethods = currentClazzMethods;
            }
            for (Method method : currentClazzMethods) {
                if (method.getAnnotation(OnOpen.class) != null) {
                    checkPublic(method);
                    if (open == null) open = method;
                    else if (currentClazz == clazz || !isMethodOverride(open, method)) {
                        throw new Exception("MethodMapping.duplicateAnnotation OnOpen");
                    }
                } else if (method.getAnnotation(OnMessage.class) != null) {
                    checkPublic(method);
                    if (message == null) message = method;
                    else if (currentClazz == clazz || !isMethodOverride(message, method)) {
                        throw new Exception("MethodMapping.duplicateAnnotation OnMessage");
                    }
                } else if (method.getAnnotation(OnBinary.class) != null) {
                    checkPublic(method);
                    if (binary == null) binary = method;
                    else if (currentClazz == clazz || !isMethodOverride(binary, method)) {
                        throw new Exception("MethodMapping.duplicateAnnotation OnBinary");
                    }
                } else if (method.getAnnotation(OnEvent.class) != null) {
                    checkPublic(method);
                    if (event == null) event = method;
                    else if (currentClazz == clazz || !isMethodOverride(event, method)) {
                        throw new Exception("MethodMapping.duplicateAnnotation OnEvent");
                    }
                } else if (method.getAnnotation(OnClose.class) != null) {
                    checkPublic(method);
                    if (close == null) close = method;
                    else if (currentClazz == clazz || !isMethodOverride(close, method)) {
                        throw new Exception("MethodMapping.duplicateAnnotation OnClose");
                    }
                } else if (method.getAnnotation(OnError.class) != null) {
                    checkPublic(method);
                    if (error == null) error = method;
                    else if (currentClazz == clazz || !isMethodOverride(error, method)) {
                        throw new Exception("MethodMapping.duplicateAnnotation OnError");
                    }
                } else {
                    // Method not annotated
                }
            }
            currentClazz = currentClazz.getSuperclass();
        }

        if (open != null && open.getDeclaringClass() != clazz) {
            if (isOverrideWithoutAnnotation(clazzMethods, open, OnOpen.class)) {
                open = null;
            }
        }
        if (message != null && message.getDeclaringClass() != clazz) {
            if (isOverrideWithoutAnnotation(clazzMethods, message, OnMessage.class)) {
                message = null;
            }
        }
        if (binary != null && binary.getDeclaringClass() != clazz) {
            if (isOverrideWithoutAnnotation(clazzMethods, binary, OnBinary.class)) {
                binary = null;
            }
        }
        if (event != null && event.getDeclaringClass() != clazz) {
            if (isOverrideWithoutAnnotation(clazzMethods, event, OnEvent.class)) {
                event = null;
            }
        }
        if (close != null && close.getDeclaringClass() != clazz) {
            if (isOverrideWithoutAnnotation(clazzMethods, close, OnClose.class)) {
                close = null;
            }
        }
        if (error != null && error.getDeclaringClass() != clazz) {
            if (isOverrideWithoutAnnotation(clazzMethods, error, OnError.class)) {
                error = null;
            }
        }

        this.onOpen = open;
        this.onMessage = message;
        this.onBinary = binary;
        this.onEvent = event;
        this.onClose = close;
        this.onError = error;

        onOpenParameters = getParameters(onOpen);
        onMessageParameters = getParameters(onMessage);
        onBinaryParameters = getParameters(onBinary);
        onEventParameters = getParameters(onEvent);
        onCloseParameters = getParameters(onClose);
        onErrorParameters = getParameters(onError);

        onOpenArgResolvers = getResolvers(onOpenParameters);
        onMessageArgResolvers = getResolvers(onMessageParameters);
        onBinaryArgResolvers = getResolvers(onBinaryParameters);
        onEventArgResolvers = getResolvers(onEventParameters);
        onCloseArgResolvers = getResolvers(onCloseParameters);
        onErrorArgResolvers = getResolvers(onErrorParameters);
    }

    private void checkPublic(Method method) throws Exception {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new Exception("MethodMapping.methodNotPublic " + method.getName());
        }
    }

    private boolean isMethodOverride(Method method1, Method method2) {
        return (method1.getName().equals(method2.getName())
                && method1.getReturnType().equals(method2.getReturnType())
                && Arrays.equals(method1.getParameterTypes(), method2.getParameterTypes()));
    }

    private boolean isOverrideWithoutAnnotation(Method[] methods, Method superclazzMethod, Class<? extends Annotation> annotation) {
        for (Method method : methods) {
            if (isMethodOverride(method, superclazzMethod) && (method.getAnnotation(annotation) == null)) {
                return true;
            }
        }
        return false;
    }


    private static MethodParameter[] getParameters(Method method) {
        if (method == null) {
            return new MethodParameter[0];
        }
        int count = method.getParameterCount();
        MethodParameter[] result = new MethodParameter[count];
        for (int i = 0; i < count; i++) {
            MethodParameter methodParameter = new MethodParameter(method, i);
            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
            result[i] = methodParameter;
        }
        return result;
    }

    public Object getEndpointInstance() {
        return applicationContext.getBean(clazz);
    }

    /**
     * OnOpen方法绑定
     */
    public Method getOnOpen() {
        return onOpen;
    }

    public Object[] getOnOpenArgs(Channel channel, HandshakeComplete handshake) throws Exception {
        return getMethodArgumentValues(channel, handshake, onOpenParameters, onOpenArgResolvers);
    }

    MethodArgumentResolver[] getOnOpenArgResolvers() {
        return onOpenArgResolvers;
    }

    /**
     * onMessage方法绑定
     */
    public Method getOnMessage() {
        return onMessage;
    }

    public Object[] getOnMessageArgs(Channel channel, TextWebSocketFrame frame) throws Exception {
        return getMethodArgumentValues(channel, frame, onMessageParameters, onMessageArgResolvers);
    }

    /**
     * onBinary方法绑定
     */
    public Method getOnBinary() {
        return onBinary;
    }

    public Object[] getOnBinaryArgs(Channel channel, BinaryWebSocketFrame frame) throws Exception {
        return getMethodArgumentValues(channel, frame, onBinaryParameters, onBinaryArgResolvers);
    }

    /**
     * onEvent方法绑定
     */
    public Method getOnEvent() {
        return onEvent;
    }

    public Object[] getOnEventArgs(Channel channel, Object evt) throws Exception {
        return getMethodArgumentValues(channel, evt, onEventParameters, onEventArgResolvers);
    }

    /**
     * onClose方法绑定
     */
    public Method getOnClose() {
        return onClose;
    }

    public Object[] getOnCloseArgs(Channel channel) throws Exception {
        return getMethodArgumentValues(channel, null, onCloseParameters, onCloseArgResolvers);
    }

    /**
     * onError方法绑定
     */
    public Method getOnError() {
        return onError;
    }

    public Object[] getOnErrorArgs(Channel channel, Throwable throwable) throws Exception {
        return getMethodArgumentValues(channel, throwable, onErrorParameters, onErrorArgResolvers);
    }

    private Object[] getMethodArgumentValues(Channel channel, Object object, MethodParameter[] parameters, MethodArgumentResolver[] resolvers) throws Exception {
        Object[] objects = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            MethodArgumentResolver resolver = resolvers[i];
            Object arg = resolver.resolveArgument(parameter, channel, object);
            objects[i] = arg;
        }
        return objects;
    }

    private MethodArgumentResolver[] getResolvers(MethodParameter[] parameters) throws Exception {
        MethodArgumentResolver[] methodArgumentResolvers = new MethodArgumentResolver[parameters.length];
        List<MethodArgumentResolver> resolvers = getDefaultResolvers();
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            for (MethodArgumentResolver resolver : resolvers) {
                if (resolver.supportsParameter(parameter)) {
                    methodArgumentResolvers[i] = resolver;
                    break;
                }
            }
            if (methodArgumentResolvers[i] == null) {
                throw new Exception("MethodMapping.paramClassIncorrect parameter name : " + parameter.getParameterName());
            }
        }
        return methodArgumentResolvers;
    }

    private List<MethodArgumentResolver> getDefaultResolvers() {
        List<MethodArgumentResolver> resolvers = new ArrayList<>();
        resolvers.add(new SessionMethodArgumentResolver());
        resolvers.add(new HttpHeadersMethodArgumentResolver());
        resolvers.add(new URIMethodArgumentResolver());
        resolvers.add(new SubProtocolMethodArgumentResolver());
        resolvers.add(new TextMethodArgumentResolver());
        resolvers.add(new ByteMethodArgumentResolver());
        resolvers.add(new EventMethodArgumentResolver(beanFactory));
        resolvers.add(new ThrowableMethodArgumentResolver());
        return resolvers;
    }
}
