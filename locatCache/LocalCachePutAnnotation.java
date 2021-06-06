package com.xkcoding.helloworld.service.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalCachePutAnnotation {

    /**
     * 提取的参数
     */
    LocalCachePutExtractParamAnnotation[] value();

    /**
     * 缓存key
     * @return
     */
    String key() default "";

    long expireTime() default 360000L;

    boolean isUpdateData() default false;

}
