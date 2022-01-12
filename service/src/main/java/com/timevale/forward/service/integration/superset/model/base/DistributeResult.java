package com.timevale.forward.service.integration.superset.model.base;

import lombok.Data;

import java.util.List;

/**
 * superset 返回集合结果对象
 *
 * @author 李涛
 * @date 2020/11/6 09:52
 */
@Data
public class DistributeResult<T> {

    private List<T> data;

    private String message;

    private Integer code;

}
