package com.sercomm.commons.umei.annotation.verb;

import static com.sercomm.commons.umei.Method.PUT;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.sercomm.commons.umei.annotation.method.DatagramMethod;

@Retention(RUNTIME)
@Target(METHOD)
@DatagramMethod(PUT)
public @interface PUT
{
    String value() default PUT;
}