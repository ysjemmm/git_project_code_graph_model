package com.timevale.forward.service.component;

import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandGroupDO;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;

import java.util.List;

/**
 * 产品需求分组组件接口
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
public interface ProductDemandGroupComponent {

    /**
     * 查询业务域需求分组列表
     * @param productDemandGroupListCondition 查询条件
     * @return 产品需求分组DO列表
     */
    List<ProductDemandGroupDO> list(ProductDemandGroupListCondition productDemandGroupListCondition);

    /**
     * 根据分组ID查询需求列表
     * @param groupId 分组ID
     * @return 产品需求分组DO列表
     */
    List<ProductDemandDO> listProductDemandByGroupId(Long groupId);

    /**
     * 根据id获取产品需求分组
     * @param id 主键id
     * @return 产品需求分组DO
     */
    ProductDemandGroupDO getById(Long id);

    /**
     * 修改产品需求分组
     * @param productDemandGroupDO 产品需求分组DO
     */
    void update(ProductDemandGroupDO productDemandGroupDO);

    /**
     * 判断项目是否已经存在
     * @param projectId 项目id
     * @return 是否存在
     */
    boolean existProject(Long projectId);

    /**
     * 取消关联项目
     */
    void removeProject(Long id);

    void linkOrUnLinkProductDemand(Long projectId, Long productDemandId, LinkOrUnLinkEnum type);

} 