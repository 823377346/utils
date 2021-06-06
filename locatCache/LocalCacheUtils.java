package com.xkcoding.helloworld.service.utils;

import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//@Data
public class LocalCacheUtils implements Comparable<LocalCacheUtils>  {


    private final static Map<String,LocalCacheDTO> cacheMap = new ConcurrentHashMap<>();
    //1GB=1073741824字节。 1KB=1024字节 1M=1024KB 1G=1024MB
    private Integer MAX_LIMIT = 1073741824;
    private float CLEARANCE_FACTOR = 0.8F;

    static {
        LocalCacheUtils localCacheUtils = new LocalCacheUtils();
        LocalCacheTimeoutThread localCacheTimeout = localCacheUtils.new LocalCacheTimeoutThread();
        new Thread(localCacheTimeout).start();
    }

    public <V> void  update(String key, V vlaue,Long expireTime){
//        checkNotNull(key,valie)
        //缓存存在更新缓存
        if(cacheMap.containsKey(key)){
            LocalCacheDTO localCacheDTO = cacheMap.get(key);
            localCacheDTO.setExpireTime(expireTime);
            localCacheDTO.setValue(vlaue);
            return;
        }
        //已经达到最大缓存
        removeMin();

        LocalCacheDTO localCacheDTO = new LocalCacheDTO();
        localCacheDTO.setHitCount(1);
        localCacheDTO.setWriteTime(Instant.now().toEpochMilli());
        localCacheDTO.setAccessTime(Instant.now().toEpochMilli());
        localCacheDTO.setExpireTime(expireTime);
        localCacheDTO.setValue(vlaue);
        cacheMap.put(key,localCacheDTO);

    }

    public <V> void  put(String key, V vlaue,Long expireTime){
//        checkNotNull(key,valie)
        //缓存存在更新缓存
        if(cacheMap.containsKey(key)){
            LocalCacheDTO localCacheDTO = cacheMap.get(key);
            localCacheDTO.setHitCount(localCacheDTO.getHitCount()+1);
            localCacheDTO.setWriteTime(Instant.now().toEpochMilli());
            localCacheDTO.setAccessTime(Instant.now().toEpochMilli());
            localCacheDTO.setExpireTime(expireTime);
            localCacheDTO.setValue(vlaue);
            return;
        }
        //已经达到最大缓存
        removeMin();

        LocalCacheDTO localCacheDTO = new LocalCacheDTO();
        localCacheDTO.setHitCount(1);
        localCacheDTO.setWriteTime(Instant.now().toEpochMilli());
        localCacheDTO.setAccessTime(Instant.now().toEpochMilli());
        localCacheDTO.setExpireTime(expireTime);
        localCacheDTO.setValue(vlaue);
        cacheMap.put(key,localCacheDTO);

    }

    private void removeMin() {
        if(isFull()){
            Object kickedKey = getKickedKey();
            if(kickedKey != null){
                cacheMap.remove(kickedKey);
                if(isFull()){
                    removeMin();
                }
            }
        }

    }

    private boolean isFull(){
        byte[] bys = null;
        try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ooo = new ObjectOutputStream(baos);
            ooo.writeObject(cacheMap);
            bys = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(bys == null){
            return true;
        }
        // 长度需要减去 4 个字节：AC ED 00 05
        // AC ED -- 魔法数字
        // 00 05 -- 版本号
        return bys.length - 4 >= MAX_LIMIT*CLEARANCE_FACTOR;
    }

    /**
     * 获取缓存值
     * @param key
     * @param <K>
     * @return
     */
    public <K> Object get(K key){
//        checkNotNull(key);
        if(cacheMap.isEmpty()) {
            return null;
        }
        if(!cacheMap.containsKey(key)){
            return null;
        }
        LocalCacheDTO localCacheDTO = cacheMap.get(key);
        if(localCacheDTO == null){
            return null;
        }
        localCacheDTO.setHitCount(localCacheDTO.getHitCount()+1);
        return localCacheDTO.getValue();
    }

    /**
     * 删除关联的key
     * @param key
     * @param <K>
     * @return
     */
    public  void delete(String key,Boolean isBatch){
        if(isBatch){
            Set<String> objects = cacheMap.keySet();
            for (String object : objects) {
                if(object.contains(key)){
                    cacheMap.remove(key);
                }
            }
            return;
        }
        cacheMap.remove(key);
    }

    /**
     * 获取最少使用的缓存
     * @return
     */
    private Object getKickedKey(){
        Collection<LocalCacheDTO> values = cacheMap.values();
        LocalCacheDTO min = Collections.min(values);
        return min.getKey() ;
    }


    @Override
    public int compareTo(LocalCacheUtils o) {
        return 0;
    }


    public static void main(String[] args) throws InterruptedException {
        Object[] objects = new Object[3];
        objects[0] = 1;
        objects[1] = "s";
        objects[2] = "aa";
        a(objects);

    }

    private static void a(Object...ars){
        for (Object ar : ars) {
            System.out.println(ar);

        }

    }

    /**
     * 处理过期缓存
     */
    public class LocalCacheTimeoutThread implements Runnable{


        @Override
        public void run() {
            try {
                TimeUnit.SECONDS.sleep(10);
                while (true){
                        TimeUnit.SECONDS.sleep(10);
                        if(cacheMap.isEmpty()){
                            continue;
                        }
                        expireCache();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private void expireCache()throws Exception{
            Set<String> keys = cacheMap.keySet();
            for (String key : keys) {

                LocalCacheDTO localCacheDTO = cacheMap.get(key);
                long timoutTime = TimeUnit.MILLISECONDS.toMillis(Instant.now().toEpochMilli() - localCacheDTO.getWriteTime());
                if(localCacheDTO.getExpireTime() > timoutTime){
                    continue;
                }
                cacheMap.remove(key);
            }

        }
    }
}
