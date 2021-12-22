package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.service.component.ProductDemandComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProductDemandComponentImpl implements ProductDemandComponent {

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Override
    public List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition) {
        return productDemandMapper.list(productDemandListCondition);
    }


}
