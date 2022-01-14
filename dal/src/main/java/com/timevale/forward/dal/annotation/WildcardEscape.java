package com.timevale.forward.dal.annotation;

import java.lang.annotation.*;

/**
 * @author by YangXu
 * @date 2022/01/13 10:18
 */
@Inherited
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WildcardEscape {
    String[] value() default {};
}
