package com.timevale.forward.service.utils;

/**
 * @auther: yuhua
 * @date: 2025/7/7 18:17
 * @description: 异常处理工具类
 */
public class ExceptionUtil {

    private ExceptionUtil() {
    }

    public static String getRootExpMsg(Throwable e) {
        if (e == null) {
            return null;
        }
        Throwable rootCause = e;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage();
    }
}
