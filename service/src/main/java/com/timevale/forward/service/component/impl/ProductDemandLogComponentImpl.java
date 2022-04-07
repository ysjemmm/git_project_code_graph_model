package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.service.component.ProductDemandLogComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProductDemandLogComponentImpl implements ProductDemandLogComponent {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Override
    public void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) {

    }

    @Override
    public void addLogAsProjectStatusChange(Map<Long, Integer> statusMap, Integer newStauts) {
        //系统 把{xx状态字段名称xx}从{原状态} 改为{新状态}
        statusMap.forEach((id,status) -> {
            if(!Objects.equals(status,newStauts)){
                BizChangeLogDO logDO = new BizChangeLogDO();
                logDO.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
                logDO.setMainId(id);
                logDO.setField(BizChangeLogFieldEnum.PRODUCT_DEMAND_STATUS.getText());
                logDO.setOldValue(BizDemandStatusEnum.getTextByCode(status));
                logDO.setNewValue(BizDemandStatusEnum.getTextByCode(newStauts));
            }
        });
    }
}
