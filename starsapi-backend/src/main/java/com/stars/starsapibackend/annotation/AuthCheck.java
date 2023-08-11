package com.stars.starsapibackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * 用于自定义权限校验的注解
 *
 * @author stars
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 具有任何一个角色的权限
     *
     * @return 角色名称的数组
     */
    String[] anyRole() default {};

    /**
     * 必须具有某个角色的权限
     *
     * @return 角色名称
     */
    String mustRole() default "";
}
