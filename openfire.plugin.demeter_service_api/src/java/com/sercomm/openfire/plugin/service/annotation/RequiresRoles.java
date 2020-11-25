package com.sercomm.openfire.plugin.service.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.ws.rs.NameBinding;

import com.sercomm.openfire.plugin.define.EndUserRole;

@NameBinding
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface RequiresRoles
{
    EndUserRole[] value() default {};
}
