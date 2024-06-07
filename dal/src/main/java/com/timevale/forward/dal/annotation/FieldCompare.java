package com.timevale.forward.dal.annotation;

import java.lang.annotation.*;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 * 用于比较两个对象的字段值
 **/
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldCompare{

    String fieldName() default "";

    Class<?> enumClass() default Enum.class;

    int scale() default 0;

    String dateFormat() default "yyyy-MM-dd";
}
