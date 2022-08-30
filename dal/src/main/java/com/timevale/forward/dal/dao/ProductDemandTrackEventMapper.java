package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProductDemandTrackEventCondition;
import com.timevale.forward.dal.entity.ProductDemandTrackEventDO;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 14:06
 */
public interface ProductDemandTrackEventMapper {

    /**
     * 查询产品业务需求关联
     *
     * @param condition condition
     * @return 列表
     */
    List<ProductDemandTrackEventDO> select(ProductDemandTrackEventCondition condition);

    /**
     * 更新单条产品需求
     *
     * @param productDemandTrackEventDO productDemandTrackEventDO
     * @return int
     */
    int update(ProductDemandTrackEventDO productDemandTrackEventDO);


    /**
     * 新增项目-产品需求
     *
     * @param productDemandTrackEventDO productDemandTrackEventDO
     * @return int
     */
    int batchInsert(List<ProductDemandTrackEventDO> productDemandTrackEventDO);


}