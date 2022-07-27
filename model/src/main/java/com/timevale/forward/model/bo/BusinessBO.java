package com.timevale.forward.model.bo;

import lombok.Data;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
public class BusinessBO {
    /**
     * 属性名字
     * */
    private String fieldName;

    /**
     * 属性值
     * */
    private List<Long> fieldValue;
}
