package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.middle.BizDemandMD;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
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
public class BizDemandLogComponentImpl implements BizDemandLogComponent {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Override
    public void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) {
        BizDemandMD oldMD = BizDemandCopier.INSTANCE.transform(oldObj);
        BizDemandMD newMD = BizDemandCopier.INSTANCE.transform(newObj);
        List<BizChangeLogDO> bizChangeLogDOList = FieldCompareUtil.commonCompare(oldMD, newMD, BizChangeLogDO.class);
    }

    @Override
    public void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active) {
        addLogWhenModifyData(oldValue, newValue, id, field, active, StringUtils.EMPTY);
    }

    @Override
    public void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action) {
        BizChangeLogDO logDO = newBizChangeLogDO(active, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());

        logDO.setMainId(id);
        logDO.setField(field);
        logDO.setOldValue(oldValue);
        logDO.setNewValue(newValue);
        if(StringUtils.isNotEmpty(action)){
            logDO.setAction(action);
        }

        bizChangeLogMapper.insert(logDO);
    }

    @Override
    public void addLogWhenBizDemandInvalid(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        List<BizDemandLinkProductDemandListDO> bizDemandLinkProductDemandListDOList = productDemandMapper.selectByBizDemandId(bizDemandId);

        // 双向变更
        List<BizChangeLogDO> bizChangeLogDOList = new ArrayList<>();
        for (BizDemandLinkProductDemandListDO e : bizDemandLinkProductDemandListDOList) {
            // 业务需求方
            BizChangeLogDO bizDemandLogDO = newBizChangeLogDO(false, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
            bizDemandLogDO.setMainId(bizDemandId);
            bizDemandLogDO.setAction(ButtonActionEnum.UN_LINK.getText());
            bizDemandLogDO.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
            bizDemandLogDO.setOldValue(e.getName());
            bizDemandLogDO.setNewValue(e.getName());

            bizChangeLogDOList.add(bizDemandLogDO);

            // 产品需求方
            BizChangeLogDO productDemandLogDO = newBizChangeLogDO(false, BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            productDemandLogDO.setMainId(e.getId());
            productDemandLogDO.setAction(ButtonActionEnum.UN_LINK.getText());
            productDemandLogDO.setField(BizChangeLogTypeEnum.BIZ_DEMAND.getText());
            productDemandLogDO.setOldValue(bizDemandDO.getName());
            productDemandLogDO.setNewValue(bizDemandDO.getName());

            bizChangeLogDOList.add(productDemandLogDO);
        }

        if (CollectionUtil.isNotEmpty(bizChangeLogDOList)) {
            bizChangeLogMapper.batchInsert(bizChangeLogDOList);
        }
    }

    @Override
    public void addLogWhenBizDemandLinkProductDemand(Long bizDemandId, List<Long> productDemandIdList) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        List<ProductDemandDO> productDemandDOList = productDemandMapper.selectByIdList(productDemandIdList);

        // 双向更新
        List<BizChangeLogDO> bizChangeLogDOList = new ArrayList<>();
        for (ProductDemandDO e : productDemandDOList) {
            // 业务需求方
            BizChangeLogDO bizDemandLogDO = newBizChangeLogDO(true, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());

            bizDemandLogDO.setMainId(bizDemandId);
            bizDemandLogDO.setAction(ButtonActionEnum.LINK.getText());
            bizDemandLogDO.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
            bizDemandLogDO.setOldValue(e.getName());
            bizDemandLogDO.setNewValue(e.getName());

            // 产品需求方
            BizChangeLogDO productDemandLogDO = newBizChangeLogDO(true, BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            productDemandLogDO.setMainId(e.getId());
            productDemandLogDO.setAction(ButtonActionEnum.LINK.getText());
            productDemandLogDO.setField(BizChangeLogTypeEnum.BIZ_DEMAND.getText());
            productDemandLogDO.setOldValue(bizDemandDO.getName());
            productDemandLogDO.setNewValue(bizDemandDO.getName());

            bizChangeLogDOList.add(productDemandLogDO);
        }

        if (CollectionUtil.isNotEmpty(bizChangeLogDOList)) {
            bizChangeLogMapper.batchInsert(bizChangeLogDOList);
        }
    }

    @Override
    public void addLogWhenBizDemandUnLinkProductDemand(Long bizDemandId, Long productDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        ProductDemandDO productDemandDO = productDemandMapper.selectById(productDemandId);

        // 业务需求方
        BizChangeLogDO bizDemandLogDO = newBizChangeLogDO(true, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());

        bizDemandLogDO.setMainId(bizDemandId);
        bizDemandLogDO.setAction(ButtonActionEnum.UN_LINK.getText());
        bizDemandLogDO.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
        bizDemandLogDO.setOldValue(productDemandDO.getName());
        bizDemandLogDO.setNewValue(productDemandDO.getName());

        // 产品需求方
        BizChangeLogDO productDemandLogDO = newBizChangeLogDO(true, BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
        productDemandLogDO.setMainId(productDemandId);
        productDemandLogDO.setAction(ButtonActionEnum.UN_LINK.getText());
        productDemandLogDO.setField(BizChangeLogTypeEnum.BIZ_DEMAND.getText());
        productDemandLogDO.setOldValue(bizDemandDO.getName());
        productDemandLogDO.setNewValue(bizDemandDO.getName());

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
                logDO.setType(BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
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

    private BizChangeLogDO newBizChangeLogDO(Boolean isUser, Integer type){
        BizChangeLogDO bizChangeLogDO = new BizChangeLogDO();
        bizChangeLogDO.setType(type);

        if(isUser){
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            bizChangeLogDO.setCreateManId(userInfo.getId());
            bizChangeLogDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        }else{
            bizChangeLogDO.setCreateManId(CommonConstant.SYSTEM);
            bizChangeLogDO.setCreateMan(CommonConstant.SYSTEM);
        }
        return bizChangeLogDO;
    }
}
