package com.xkcoding.helloworld.service.utils;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = LocalCachePutAnnotation.class)
public @interface LocalCachePutExtractParamAnnotation {

    /**
     * 作为锁的参数名称
     */
    String paramName();

    /**
     * 参数的 属性值。可以为空
     */
    String fieldName() default "";
}
