package com.magfin.idempotent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;

/**
 * 自动配置
 *
 * @author yewub
 *
 */
@Configuration
@EnableConfigurationProperties({IdempotentProperties.class, RedisProperties.class})
@ConditionalOnClass()
@ConditionalOnProperty(prefix = "spring.idempotent", value = "enabled", matchIfMissing = true)
public class IdempotentAutoConfiguration {
    @Autowired
    private RedisProperties redisProperties;

    @Bean
    @ConditionalOnMissingBean(name = "jedisPool")
    public JedisPool redisPoolFactory() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        Duration timeout = redisProperties.getTimeout();
        int timeoutInt = 5000;
        if (timeout != null) {
            timeoutInt = (int) timeout.getSeconds();
        }
        return new JedisPool(jedisPoolConfig, redisProperties.getHost(),
                redisProperties.getPort(),
                timeoutInt,
                redisProperties.getPassword(),
                redisProperties.getDatabase());
    }

}
