package com.magfin.idempotent;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 幂等性拦截配置
 *
 * @author yewub
 */
@ConfigurationProperties(prefix = "spring.idempotent")
public class IdempotentProperties {
    /**
     * 是否启用
     */
    private boolean isEnable = false;
    /**
     * 默认幂等性生效时间间隔
     */
    private long defaultExpireTime = 0;


    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    public long getDefaultExpireTime() {
        return defaultExpireTime;
    }

    public void setDefaultExpireTime(long defaultExpireTime) {
        this.defaultExpireTime = defaultExpireTime;
    }
}
