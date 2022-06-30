package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProductDemandTrackEventCondition;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandTrackEventMapper;
import com.timevale.forward.dal.entity.ProductDemandTrackEventDO;
import com.timevale.forward.service.component.ProductDemandTrackEventComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProductDemandTrackEventComponentImpl implements ProductDemandTrackEventComponent {

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private ProductDemandTrackEventMapper productDemandTrackEventMapper;



    @Override
    public void update(Long productDemandId, Long trackEventId) {
        log.info("删除产品与事件关系,productDemandId={},trackEventId={}", productDemandId, trackEventId);
        ProductDemandTrackEventDO productDemandTrackEventDO = new ProductDemandTrackEventDO();
        productDemandTrackEventDO.setIsDeleted(true);
        productDemandTrackEventDO.setProductDemandId(productDemandId);
        productDemandTrackEventDO.setTrackEventId(trackEventId);
        productDemandTrackEventMapper.update(productDemandTrackEventDO);
    }

    @Override
    public void batchInsert(Long productDemandId, List<Long> trackEventIds) {
        log.info("新增产品与事件关系,productDemandId={},trackEventIds={}", productDemandId, trackEventIds);
        if (CollectionUtils.isEmpty(trackEventIds)) {
            return;
        }

        ProductDemandTrackEventCondition c = ProductDemandTrackEventCondition.builder().productDemandId(productDemandId).isDeleted(false).build();
        List<ProductDemandTrackEventDO> exists = productDemandTrackEventMapper.select(c);
        List<Long> existTrackEventIds = exists.stream().map(ProductDemandTrackEventDO::getTrackEventId).collect(Collectors.toList());
        trackEventIds.removeAll(existTrackEventIds);
        // link
        if (!CollectionUtils.isEmpty(trackEventIds)) {
            List<ProductDemandTrackEventDO> list = trackEventIds.stream().map(i -> {
                ProductDemandTrackEventDO a = new ProductDemandTrackEventDO();
                a.setProductDemandId(productDemandId);
                a.setTrackEventId(i);
                return a;
            }).collect(Collectors.toList());
            productDemandTrackEventMapper.batchInsert(list);
        }
    }

}
