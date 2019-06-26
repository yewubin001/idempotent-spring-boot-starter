package com.magfin.idempotent.aspect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

/**
 * @author yewub
 * @Date: 2019/5/20 17:01
 * @Description:
 */
@Component
public class RedisLock {
    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final Long RELEASE_SUCCESS = 1L;
    private static final String LOCK_KEY_PREFIX = "lock:";
    @Autowired
    private JedisPool jedisPool;

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey    锁
     * @param requestId  请求标识
     * @param expireTime 超期时间
     * @return 是否获取成功
     */
    public boolean tryGetDistributedLock(String lockKey, String requestId, int expireTime) {
        try (Jedis jedis = getJedis()) {
            String result = jedis.set(getLockKey(lockKey), requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
            return LOCK_SUCCESS.equalsIgnoreCase(result);
        }
    }

    /**
     * 释放分布式锁
     *
     * @param lockKey   锁
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    public boolean releaseDistributedLock(String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        try (Jedis jedis = getJedis()) {
            Object result = jedis.eval(script, Collections.singletonList(getLockKey(lockKey)),
                    Collections.singletonList(requestId));
            return RELEASE_SUCCESS.equals(result);
        }
    }

    private String getLockKey(String lockKey) {
        return LOCK_KEY_PREFIX + lockKey;
    }


    public Jedis getJedis() {
        return jedisPool.getResource();
    }
}