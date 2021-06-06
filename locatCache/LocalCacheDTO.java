package com.xkcoding.helloworld.service.utils;

import java.io.Serializable;
import java.lang.reflect.Method;

public class LocalCacheDTO implements Comparable<LocalCacheDTO>,Serializable {

    private Object key;

    private Object value;

    //最后访问时间
    private Long accessTime;

    //创建时间
    private long writeTime;

    //存活时间
    private long expireTime;

    //命中时间
    private Integer hitCount;

//    //请求参数
//    private Object[] reqParams;
//
//    //获取数据方法
//    private Method targetMethod;

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Long getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(Long accessTime) {
        this.accessTime = accessTime;
    }

    public long getWriteTime() {
        return writeTime;
    }

    public void setWriteTime(long writeTime) {
        this.writeTime = writeTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getHitCount() {
        return hitCount == null ? 0 :hitCount;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    @Override
    public int compareTo(LocalCacheDTO o) {
        return 0;
    }
}
