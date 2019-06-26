package com.magfin.idempotent.aspect;

import java.io.Serializable;

/**
 * 空的返回值占位符，用于void返回值或null对象的redis缓存站位
 *
 * @author yewub
 * @Date: 2019/5/25 21:22
 */
public class NullReturnPlaceholder implements Serializable {
    private boolean placeholder;

    public boolean isPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
    }

    public NullReturnPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
    }

    public NullReturnPlaceholder() {
    }

    private static final NullReturnPlaceholder instance = new NullReturnPlaceholder(true);

    public static NullReturnPlaceholder getInstance() {
        return instance;
    }
}
