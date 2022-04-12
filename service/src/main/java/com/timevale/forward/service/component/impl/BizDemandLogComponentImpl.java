package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class BizDemandLogComponentImpl implements BizDemandLogComponent {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Override
    public void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) {
    }

    @Override
    public void addLogAsProductDemandStatusChange(Map<Long, Integer> oldStautsMap, Map<Integer, List<Long>> newStautsMap) {
        Map<Long, Integer> newStautsChangeMap = new HashMap<>();
        newStautsMap.forEach((status, ids) -> {
            ids.forEach(id -> {
                newStautsChangeMap.put(id, status);
            });
        });
        List<BizChangeLogDO> logs = new ArrayList<>();
        oldStautsMap.forEach((id, oldStatus) -> {
            if (newStautsChangeMap.containsKey(id) && !Objects.equals(oldStatus, newStautsChangeMap.get(id))) {
                BizChangeLogDO logDO = new BizChangeLogDO();
                logDO.setType(BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
                logDO.setMainId(id);
                logDO.setField(BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText());
                logDO.setOldValue(BizDemandStatusEnum.getTextByCode(oldStatus));
                logDO.setNewValue(BizDemandStatusEnum.getTextByCode(newStautsChangeMap.get(id)));
                logDO.setCreateMan(CommonConstant.SYSTEM);
                logDO.setCreateManId(CommonConstant.SYSTEM);
                logs.add(logDO);
            }
        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }
}
