package com.timevale.forward.service.utils.aop;


import java.lang.annotation.*;

/**
 * @author by YangXu
 * @date 2022/03/25 15:53
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.TYPE})
public @interface LogPoint {
    /**
     * 日志描述
     */
    String description() default "";
}
