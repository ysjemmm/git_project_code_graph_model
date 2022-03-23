package com.timevale.forward.model.middle;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Date 2022/3/22 19:04
 * @Author 望轩
 */
@Data
public class BusinessBeanMD implements Serializable {
    /**
     * 属性名字
     * */
    private String fieldName;

    /**
     * 属性值
     * */
    private List<Long> fieldValue;
}