package com.timevale.forward.dal.annotation;

import java.lang.annotation.*;

/**
 * @author by YangXu
 * @date 2022/01/13 10:18
 *
 * mybatis模糊查询，通配运算符转义处理
 */
@Inherited
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WildcardEscape {

}
