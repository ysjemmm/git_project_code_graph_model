package com.timevale.forward.service.integration.superset.model.base;

import lombok.Data;

import java.util.List;

/**
 * superset 查询分页结果
 *
 * @author 李涛
 * @date 2020/11/6 09:49
 */
@Data
public class PageResult<T> {

    /**
     * 查询结果
     */
    private List<T> result;

    /**
     * 总数
     */
    private Integer total;

}
