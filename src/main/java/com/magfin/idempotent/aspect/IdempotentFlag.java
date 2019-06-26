package com.magfin.idempotent.aspect;

/**
 * 幂等性标记
 *
 * @Auther: yewub
 * @Date: 2019/5/25 21:21
 */
public class IdempotentFlag {
    private static ThreadLocal<Boolean> idempotent = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static void doing() {
        idempotent.set(Boolean.TRUE);
    }

    public static boolean isDoing() {
        return idempotent.get();
    }

    public static void done() {
        idempotent.remove();
    }
}