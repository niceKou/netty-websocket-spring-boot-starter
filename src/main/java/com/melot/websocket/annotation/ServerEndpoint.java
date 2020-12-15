package com.melot.websocket.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ServerEndpoint {

    String host() default "0.0.0.0";

    int port() default 20000;

    String path() default "/";

    int iothreads() default -1;

    int accepts() default 0;

    int heartbeat() default 30000;

    int readerIdleTime() default 0;

    int writeIdleTime() default 0;

    int allIdleTime() default 60;

    int maxContentLength() default 8192;
}