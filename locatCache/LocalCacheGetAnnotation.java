package com.xkcoding.helloworld.service.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalCacheGetAnnotation {

    /**
     * 提取的参数
     */
    LocalCacheGetExtractParamAnnotation[] value();

    /**
     * 缓存key
     * @return
     */
    String key() default "";

    /**
     * 是否更新缓存数据 默认 false
     * @return
     */
    boolean isUpdateData() default true;

    long expireTime() default 360000L;
}
