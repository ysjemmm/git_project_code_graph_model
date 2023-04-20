package com.timevale.forward.service.component;

import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;

import java.util.List;

public interface ProductDemandComponent {
    /**
     *
     * @param productDemandListCondition 查询条件
     * @return 列表
     */
    List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition);

    /**
     *
     * @param id 查询条件
     * @return 详情
     */
    ProductDemandDetailVO get(Long id);

    /**
     *
     * @param productDemandDO 产品需求
     */
    void update(ProductDemandDO productDemandDO);

    /**
     *
     * @param projectId 项目id
     * @param status 项目状态
     */
    void updateProductDemandStatus(Long projectId,Integer status);

    /**
     *
     * @param projectId 项目id
     * @param status 项目状态
     */
    void updateProductDemandStatus(Long projectId,Integer status,List<Long>productDemandIds);

    /**
     *
     * @param productDemandIds 产品需求id
     * @param invalid 产品需求与关联业务需求取消关联,
     */
    void updateDemandStatusAsProductStatusChange(List<Long> productDemandIds,boolean invalid);

    /**
     *
     * @param demandId demandId
     * @param productDemandId productDemandId
     */
    void updateDemandStatusWhenUnlink(Long demandId,Long productDemandId,boolean bizDemand);

    /**
     *
     * @param productStatus productStatus 产品需求状态
     * @param bizDemandId bizDemandId
     * @param bizDemand bizDemand
     */
    Integer updateDemandStatus(Integer productStatus, Long bizDemandId,boolean bizDemand);

    /**
     *
     * @param productDemandIds productDemandIds
     * @param invalid invalid
     */
    void updateBizDemandStatus(List<Long> productDemandIds, boolean invalid);

    /**
     *
     * @param productDemandIds productDemandIds
     * @param invalid invalid
     */
    void updateCustomDemandStatus(List<Long> productDemandIds, boolean invalid);


    /**
     *
     * @param productDemandIds productDemandIds
     * @return Long
     */
    List<Long> getLinkBizDemandIds(List<Long> productDemandIds);

    /**
     *
     * @param productDemandIds productDemandIds
     * @return Long
     */
    List<Long> getLinkCustomDemandIds(List<Long> productDemandIds);

    void sendDingMsg(Integer oldStatus, Integer newStatus, Long bizDemandId);

}
