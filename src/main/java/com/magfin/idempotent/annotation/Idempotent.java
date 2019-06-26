package com.magfin.idempotent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Auther: yewub
 * @Date: 2019/5/25 21:17
 * @Description: 自定义注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Idempotent {
    /**
     * 幂等key的分组值，例如：sale-check
     *
     * @return
     */
    String value() default "";

    /**
     * 参数的表达式，用来确定key值，例如："#req.saleInfo.channelCode+'-'+#req.seqId"，
     * 如果值为null或""，则取方法的所有参数json值的md5哈希值作为幂等key的一部分
     *
     * @return
     */
    String express();

    /**
     * 失效时间，单位为秒
     *
     * @return
     */
    long expireTime() default 0;
}