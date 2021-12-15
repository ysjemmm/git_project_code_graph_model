package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProductLineDO;

import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/15 10:41
 */
public interface ProductLineMapper {

    /**
     * 获取产品线列表
     *
     * @return 列表
     */
    List<ProductLineDO> selectAllProductLine();
}
