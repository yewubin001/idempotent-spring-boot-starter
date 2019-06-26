package com.magfin.idempotent.utils;


/**
 * @author yewub
 */
public final class JSONException extends RuntimeException {

    static final String GET_VALUE_ERROR = "获取json中key值异常";

    public JSONException(final String message) {
        super(message);
    }

    public JSONException(final String message, final Throwable cause) {
        super(message, cause);
    }
}