package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.CustomDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.service.component.CustomDemandLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class CustomDemandLogComponentImpl implements CustomDemandLogComponent {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private CustomDemandMapper customDemandMapper;

    @Override
    public void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active) {
        addLogWhenModifyData(oldValue, newValue, id, field, active, StringUtils.EMPTY);
    }

    @Override
    public void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action) {
        if (!Objects.equals(oldValue, newValue)) {
            BizChangeLogDO logDO = newCustomChangeLogDO(active, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());
            logDO.setMainId(id);
            logDO.setField(field);
            logDO.setOldValue(oldValue);
            logDO.setNewValue(newValue);
            if (StringUtils.isNotEmpty(action)) {
                logDO.setAction(action);
            }
            bizChangeLogMapper.insert(logDO);
        }
    }

    @Override
    public void addLogWhenCustomDemandLinkProductDemand(Long customDemandId, List<Long> productDemandIdList) {
        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandId);
        List<ProductDemandDO> productDemandDOList = productDemandMapper.selectByIdList(productDemandIdList);

        // 双向更新
        List<BizChangeLogDO> bizChangeLogDOList = new ArrayList<>();
        for (ProductDemandDO e : productDemandDOList) {
            // 业务需求方
            BizChangeLogDO bizDemandLogDO = newCustomChangeLogDO(true, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());

            bizDemandLogDO.setMainId(customDemandId);
            bizDemandLogDO.setAction(ButtonActionEnum.LINK.getText());
            bizDemandLogDO.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
            bizDemandLogDO.setOldValue(e.getName());
            bizDemandLogDO.setNewValue(e.getName());

            bizChangeLogDOList.add(bizDemandLogDO);

            // 产品需求方
            BizChangeLogDO productDemandLogDO = newCustomChangeLogDO(true, BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            productDemandLogDO.setMainId(e.getId());
            productDemandLogDO.setAction(ButtonActionEnum.LINK.getText());
            productDemandLogDO.setField(BizChangeLogTypeEnum.CUSTOM_DEMAND.getText());
            productDemandLogDO.setOldValue(customDemandDO.getName());
            productDemandLogDO.setNewValue(customDemandDO.getName());

            bizChangeLogDOList.add(productDemandLogDO);
        }

        if (CollectionUtil.isNotEmpty(bizChangeLogDOList)) {
            bizChangeLogMapper.batchInsert(bizChangeLogDOList);
        }
    }

    @Override
    public void addLogWhenCustomDemandUnLinkProductDemand(Long customDemandId, Long productDemandId) {
        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandId);
        ProductDemandDO productDemandDO = productDemandMapper.selectById(productDemandId);

        // 业务需求方
        BizChangeLogDO bizDemandLogDO = newCustomChangeLogDO(true, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());

        bizDemandLogDO.setMainId(customDemandId);
        bizDemandLogDO.setAction(ButtonActionEnum.UN_LINK.getText());
        bizDemandLogDO.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
        bizDemandLogDO.setOldValue(productDemandDO.getName());
        bizDemandLogDO.setNewValue(productDemandDO.getName());

        // 产品需求方
        BizChangeLogDO productDemandLogDO = newCustomChangeLogDO(true, BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
        productDemandLogDO.setMainId(productDemandId);
        productDemandLogDO.setAction(ButtonActionEnum.UN_LINK.getText());
        productDemandLogDO.setField(BizChangeLogTypeEnum.CUSTOM_DEMAND.getText());
        productDemandLogDO.setOldValue(customDemandDO.getName());
        productDemandLogDO.setNewValue(customDemandDO.getName());

        // 添加日志
        List<BizChangeLogDO> bizChangeLogDOList = new ArrayList<>();
        bizChangeLogDOList.add(bizDemandLogDO);
        bizChangeLogDOList.add(productDemandLogDO);

        if (CollectionUtil.isNotEmpty(bizChangeLogDOList)) {
            bizChangeLogMapper.batchInsert(bizChangeLogDOList);
        }
    }


    @Override
    public void addLogAsProductDemandStatusChange(Map<Long, Integer> oldStatusMap, Map<Integer, List<Long>> newStatusMap) {
        Map<Long, Integer> newStatusChangeMap = new HashMap<>();
        newStatusMap.forEach((status, ids) -> {
            ids.forEach(id -> {
                newStatusChangeMap.put(id, status);
            });
        });
        List<BizChangeLogDO> logs = new ArrayList<>();
        oldStatusMap.forEach((id, oldStatus) -> {
            if (newStatusChangeMap.containsKey(id) && !Objects.equals(oldStatus, newStatusChangeMap.get(id))) {
                BizChangeLogDO logDO = new BizChangeLogDO();
                logDO.setType(BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());
                logDO.setMainId(id);
                logDO.setField(BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText());
                logDO.setOldValue(BizDemandStatusEnum.getTextByCode(oldStatus));
                logDO.setNewValue(BizDemandStatusEnum.getTextByCode(newStatusChangeMap.get(id)));
                logDO.setCreateMan(CommonConstant.SYSTEM);
                logDO.setCreateManId(CommonConstant.SYSTEM);
                logs.add(logDO);
            }
        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }

    @Override
    public void addLogAsProductDemandStatusChange(Long id, Integer oldStatus, Integer newStatus) {
        if (!Objects.equals(oldStatus, newStatus)) {
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());
            logDO.setMainId(id);
            logDO.setField(BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText());
            logDO.setOldValue(BizDemandStatusEnum.getTextByCode(oldStatus));
            logDO.setNewValue(BizDemandStatusEnum.getTextByCode(newStatus));
            logDO.setCreateMan(CommonConstant.SYSTEM);
            logDO.setCreateManId(CommonConstant.SYSTEM);
            bizChangeLogMapper.insert(logDO);
        }
    }

    @Override
    public BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active) {
        return getLogWhenModifyData(oldValue, newValue, id, field, active, "");
    }

    @Override
    public BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action) {
        BizChangeLogDO logDO = newCustomChangeLogDO(active, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());

        logDO.setMainId(id);
        logDO.setField(field);
        logDO.setOldValue(oldValue);
        logDO.setNewValue(newValue);
        if (StringUtils.isNotEmpty(action)) {
            logDO.setAction(action);
        }

        return logDO;
    }

    @Override
    public BizChangeLogDO buildLogWhenPublishDateChange(String oldValue, String newValue, Long id) {
        BizChangeLogDO logDO = newCustomChangeLogDO(false, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode());
        logDO.setMainId(id);
        logDO.setField(BizChangeLogFieldEnum.PROJECT_RELEASE_DATE.getText());
        logDO.setOldValue(oldValue);
        logDO.setNewValue(newValue);
        return logDO;
    }

    private BizChangeLogDO newCustomChangeLogDO(Boolean isUser, Integer type) {
        BizChangeLogDO bizChangeLogDO = new BizChangeLogDO();
        bizChangeLogDO.setType(type);

        if (isUser) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            bizChangeLogDO.setCreateManId(userInfo.getId());
            bizChangeLogDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        } else {
            bizChangeLogDO.setCreateManId(CommonConstant.SYSTEM);
            bizChangeLogDO.setCreateMan(CommonConstant.SYSTEM);
        }
        return bizChangeLogDO;
    }
}
