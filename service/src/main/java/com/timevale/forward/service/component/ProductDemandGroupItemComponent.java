package com.timevale.forward.service.component;

// 引入必要的包
import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.dto.ProductDemandMoveDTO;
import com.timevale.forward.dal.entity.ProductDemandGroupItemDO;
import com.timevale.forward.dal.entity.ProductDemandGroupItemListDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.request.ProductDemandGroupItemMoveReq;

import java.util.List;
import java.util.function.BiFunction;

/**
 * 产品-分组关系表 组件接口
 */
public interface ProductDemandGroupItemComponent {
    // 删除某分组下产品需求
    void deleteByGroupId(Long groupId);
    // 查询某分组下产品需求
    List<ProductDemandGroupItemListDO> listProductDemand(ProductDemandGroupListCondition productDemandGroupListCondition);
    // 查询业务域下待规划的产品需求
    List<ProductDemandListDO> listProductDemandBacklog(ProductDemandGroupListCondition productDemandGroupListCondition);

    ProductDemandGroupItemDO getByGroupIdAndDemandId(Long groupId, Long demandId);

    ProductDemandMoveDTO moveProductDemand(ProductDemandGroupItemMoveReq productDemandGroupItemMoveReq);
    double calculateNewPosition(Long prevId,
                                Double prevPosition,
                                Long nextId,
                                Double nextPosition,
                                Long bizDomainId,
                                Long positionTarget,
                                BiFunction<Long, Double, Double> getPrevByPosition,
                                BiFunction<Long, Double, Double> getNextByPosition);
}