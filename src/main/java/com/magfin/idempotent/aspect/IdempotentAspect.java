package com.magfin.idempotent.aspect;

import com.magfin.idempotent.IdempotentProperties;
import com.magfin.idempotent.annotation.Idempotent;
import com.magfin.idempotent.utils.ExpressionUtil;
import com.magfin.idempotent.utils.JSONUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: yewub
 * @Date: 2019/5/25 21:17
 * @Description:
 * Spring AOP 通过order来指定顺序
 * order 的值越小，说明越先被执行order 的值越小，说明越先被执行
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Aspect
@Component
public class IdempotentAspect {
    public static final String RESULT_CACHE_KEY_PREFIX = "com:magfin:idempotent:value:";
    public static final String LOCK_KEY_PREFIX = "com:magfin:idempotent:lock:";
    /**
     * 锁失效时间，程序崩溃的情况，锁失效时间
     */
    public static final int REDIS_LOCK_EXPIRE_TIME = 30 * 1000;
    private static final long DEFAULT_EXPIRE_TIME = 60 * 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(IdempotentAspect.class);
    public static final int MIN_CACHE_EXPIRE_TIME = 5;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisLock redisLock;
    @Autowired
    private IdempotentProperties idempotentProperties;

    /**
     * 定义切入点为 带有 Idempotent 注解的
     */
    @Pointcut("@annotation(com.magfin.idempotent.annotation.Idempotent)")
    public void idempotent() {
    }

    @Around("idempotent()")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (!idempotentProperties.isEnable()) {
            try {
                return joinPoint.proceed(args);
            } catch (Throwable throwable) {
                throw throwable;
            }
        }
        LOGGER.debug("开始进行幂等性验证");
        String[] params = ((CodeSignature) joinPoint.getStaticPart().getSignature()).getParameterNames();
        if (params == null || params.length == 0) {
            throw new RuntimeException("无法进行幂等性校验");
        }
        Object result;
        if (!IdempotentFlag.isDoing()) {
            try {
                IdempotentFlag.doing();
                Map<String, Object> methodParaMap = getParamsMap(params, args);
                MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                Method method = methodSignature.getMethod();
                Idempotent idempotent = method.getAnnotation(Idempotent.class);
                String key = getKeyString(args, methodParaMap, idempotent);
                String group = getGroupName(joinPoint, idempotent);
                long cacheExpireTime = getExpireTime(idempotent);
                // 利用redisLock加锁获取结果
                result = getResult(joinPoint, group, key, cacheExpireTime);
            } finally {
                IdempotentFlag.done();
            }
        } else {
            try {
                result = joinPoint.proceed(args);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    public String getKeyString(Object[] args, Map<String, Object> methodParaMap, Idempotent idempotent) {
        String key;
        if (StringUtils.isEmpty(idempotent.express())) {
            key = args2key(args);
        } else {
            key = ExpressionUtil.parseExpression(methodParaMap, idempotent.express());
            if (StringUtils.isEmpty(key)) {
                key = args2key(args);
            }
        }
        if (StringUtils.isEmpty(key)) {
            throw new RuntimeException("根据Idempotent表达式获得不到有消息的key");
        }
        return key;
    }

    public String getGroupName(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        String group;
        if (StringUtils.isEmpty(idempotent.value())) {
            group = joinPoint.getSignature().getDeclaringTypeName() + "-" + joinPoint.getSignature().getName();
        } else {
            group = idempotent.value();
        }
        return group;
    }

    private String args2key(Object[] args) {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(args).forEach(i -> sb.append(JSONUtil.toJson(i)));
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private long getExpireTime(Idempotent idempotent) {
        if (idempotent.expireTime() > MIN_CACHE_EXPIRE_TIME) {
            return idempotent.expireTime();
        }
        if (idempotentProperties.getDefaultExpireTime() > MIN_CACHE_EXPIRE_TIME) {
            return idempotentProperties.getDefaultExpireTime();
        }
        return DEFAULT_EXPIRE_TIME;
    }

    private Map<String, Object> getParamsMap(String[] params, Object[] args) {
        Map<String, Object> methodParaMap = new HashMap<>(params.length);
        for (int i = 0; i < params.length; i++) {
            methodParaMap.put(params[i], args[i]);
        }
        return methodParaMap;
    }

    public Object getResult(ProceedingJoinPoint joinPoint, String idemGroup, String idemKey, long expireTime) throws Throwable {
        String resultCacheKey = getResultCacheKey(idemGroup, idemKey);
        String lockKey = getResultLockKey(idemGroup, idemKey);
        Object result = redisTemplate.opsForValue().get(resultCacheKey);
        String requestId = UUID.randomUUID().toString();
        if (result == null) {
            try {
                // 以请求参数作为唯一性的key进行加锁
                if (redisLock.tryGetDistributedLock(lockKey, requestId, REDIS_LOCK_EXPIRE_TIME)) {
                    // 需要加锁的代码
                    result = redisTemplate.opsForValue().get(resultCacheKey);
                    if (result != null) {
                        LOGGER.warn("幂等性校验重复的请求，返回缓存的结果");
                        return result;
                    }
                    result = joinPoint.proceed(joinPoint.getArgs());
                    if (result == null) {
                        result = NullReturnPlaceholder.getInstance();
                    }
                    redisTemplate.opsForValue().set(resultCacheKey, result, expireTime, TimeUnit.SECONDS);
                }
            } catch (Throwable e) {
                throw e;
            } finally {
                // 释放锁
                redisLock.releaseDistributedLock(lockKey, requestId);
            }
        }
        if (result instanceof NullReturnPlaceholder) {
            return null;
        } else {
            return result;
        }
    }

    private String getResultLockKey(String idemGroup, String idemKey) {
        return LOCK_KEY_PREFIX + idemGroup + ":" + idemKey;
    }

    private String getResultCacheKey(String idemGroup, String idemKey) {
        return RESULT_CACHE_KEY_PREFIX + idemGroup + ":" + idemKey;
    }

    /**
     * springboot是无法自动注入RestTemplate ，但是springCloud可以自动注入，
     * 我们要初始化springboot配置类中注入RestTemplate
     * 这样子在其他class中 @Autowired RestTemplate restTemplate就不会有问题了
     */
//    @Bean
//    public RedisTemplate redisTemplateInit() {
//        //设置序列化Key的实例化对象
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        //设置序列化Value的实例化对象
//        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//        return redisTemplate;
//    }
}
