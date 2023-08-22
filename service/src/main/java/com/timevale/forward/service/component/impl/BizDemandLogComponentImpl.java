package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.BizDemandMD;
import com.timevale.forward.service.component.BizDemandComponent;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private BizDemandComponent bizDemandComponent;
    @Resource
    private ProductDemandMapper productDemandMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private BizDemandMapper bizDemandMapper;

    @Override
    public void addLogWhenModifyData(BizDemandDO oldObj, BizDemandDO newObj) {
        Long id = oldObj.getId();
        BizDemandMD oldMD = BizDemandCopier.INSTANCE.transform(oldObj);
        BizDemandMD newMD = BizDemandCopier.INSTANCE.transform(newObj);
        List<BizChangeLogDO> bizChangeLogDOList = FieldCompareUtil.commonCompare(oldMD, newMD, BizChangeLogDO.class);
        // 部门，产品线 判断
        if (!Objects.equals(oldObj.getDeptId(), newObj.getDeptId())) {
            String oldDeptName = bizDemandComponent.getDeptChainName(oldObj.getDeptId());
            String newDeptName = bizDemandComponent.getDeptChainName(newObj.getDeptId());
            addLogWhenModifyData(oldDeptName, newDeptName, id, BizChangeLogFieldEnum.DEPARTMENT.getText(), true);
        }
        if (!Objects.equals(oldObj.getProductLineId(), newObj.getProductLineId())) {
            String oldProductLineName = productLineMapper.selectById(oldObj.getProductLineId()).getName();
            String newProductLineName = productLineMapper.selectById(newObj.getProductLineId()).getName();
            addLogWhenModifyData(oldProductLineName, newProductLineName, id, BizChangeLogFieldEnum.PRODUCT_LINE.getText(), true);
        }
        if (CollectionUtil.isNotEmpty(bizChangeLogDOList)) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            for (BizChangeLogDO bizChangeLogDO : bizChangeLogDOList) {
                bizChangeLogDO.setCreateManId(userInfo.getId());
                bizChangeLogDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            }
            bizChangeLogMapper.batchInsert(bizChangeLogDOList);
        }
    }

    @Override
    public void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active) {
        addLogWhenModifyData(oldValue, newValue, id, field, active, StringUtils.EMPTY);
    }

    @Override
    public void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action) {
        addLogWhenModifyData(oldValue, newValue, id, field, active, action, StringUtils.EMPTY);
    }

    @Override
    public void addLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action, String identity) {
        BizChangeLogDO logDO = newBizChangeLogDO(active, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());

        logDO.setMainId(id);
        logDO.setField(field);
        logDO.setOldValue(oldValue);
        logDO.setNewValue(newValue);
        if (StringUtils.isNotEmpty(action)) {
            logDO.setAction(action);
        }
        if (StringUtils.isNotEmpty(identity)) {
            logDO.setIdentity(identity);
        }

        bizChangeLogMapper.insert(logDO);
    }

    @Override
    public void addLogWhenBizDemandInvalid(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
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
    public void linkPd(Long bizDemandId, Collection<Long> productDemandIdList) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
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

            bizChangeLogDOList.add(bizDemandLogDO);

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
    public void unlinkPd(Long bizDemandId, Long productDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
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
    public void linkProject(ProjectDO project, Collection<BizDemandDO> bizDemands) {

        // 业务需求关联项目
        List<BizChangeLogDO> bizChangeLogs =
                bizDemands.stream().map(bd -> newBizChangeLogDO(true, BizChangeLogTypeEnum.BIZ_DEMAND.getCode())
                        .setMainId(bd.getId())
                        .setAction(ButtonActionEnum.LINK.getText()).setField(BizChangeLogTypeEnum.PROJECT.getText())
                        .setOldValue(project.getName())
                        .setNewValue(project.getName())).collect(Collectors.toList());

        // 项目关联业务需求列表
        bizChangeLogs.addAll(bizDemands.stream().map(bd -> newBizChangeLogDO(true, BizChangeLogTypeEnum.PROJECT.getCode())
                .setMainId(project.getId())
                .setAction(ButtonActionEnum.LINK.getText())
                .setField(BizChangeLogTypeEnum.BIZ_DEMAND.getText())
                .setOldValue(bd.getName())
                .setNewValue(bd.getName())).collect(Collectors.toList()));
        bizChangeLogMapper.batchInsert(bizChangeLogs);

    }

    @Override
    public void unlinkProject(ProjectDO project, Collection<BizDemandDO> bizDemands) {
        List<BizChangeLogDO> bizChangeLogs =
                bizDemands.stream().map(bizDemandDO ->
                newBizChangeLogDO(true, BizChangeLogTypeEnum.BIZ_DEMAND.getCode())
                .setMainId(bizDemandDO.getId())
                .setAction(ButtonActionEnum.UN_LINK.getText())
                .setField(BizChangeLogTypeEnum.PROJECT.getText())
                .setOldValue(project.getName())
                .setNewValue(project.getName())).collect(Collectors.toList());
        bizChangeLogs.addAll(bizDemands.stream().map(bizDemandDO ->
                newBizChangeLogDO(true, BizChangeLogTypeEnum.PROJECT.getCode())
                .setMainId(project.getId())
                .setAction(ButtonActionEnum.UN_LINK.getText())
                .setField(BizChangeLogTypeEnum.BIZ_DEMAND.getText())
                .setOldValue(bizDemandDO.getName())
                .setNewValue(bizDemandDO.getName())).collect(Collectors.toList()));
        bizChangeLogMapper.batchInsert(bizChangeLogs);
    }

    @Override
    public void addLogAsProductDemandStatusChange(Integer oldStatus, Integer newStatus, Long id, Integer type) {
        if (!Objects.equals(oldStatus, newStatus)) {
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(type);
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
    public void addLogsAsProjectStatusChange(Integer oldStatus, Integer newStatus, Collection<Long> ids) {
        if (Objects.equals(oldStatus, newStatus)) {
            return;
        }
        List<BizChangeLogDO> logs = ids.stream().map(id -> {
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
            logDO.setMainId(id);
            logDO.setField(BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText());
            logDO.setOldValue(BizDemandStatusEnum.getTextByCode(oldStatus));
            logDO.setNewValue(BizDemandStatusEnum.getTextByCode(newStatus));
            logDO.setCreateMan(CommonConstant.SYSTEM);
            logDO.setCreateManId(CommonConstant.SYSTEM);
            return logDO;
        }).collect(Collectors.toList());
        bizChangeLogMapper.batchInsert(logs);
    }

    @Override
    public BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active) {
        return getLogWhenModifyData(oldValue, newValue, id, field, active, "");
    }

    @Override
    public BizChangeLogDO getLogWhenModifyData(String oldValue, String newValue, Long id, String field, Boolean active, String action) {
        BizChangeLogDO logDO = newBizChangeLogDO(active, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());

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
        return buildLogWhenPublishDateChange(oldValue, newValue, id, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
    }

    @Override
    public BizChangeLogDO buildLogWhenStatusChange(String oldValue, String newValue, Long id) {
        BizChangeLogDO log = newBizChangeLogDO(false, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
        log.setMainId(id);
        log.setField(BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText());
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        return log;
    }


    @Override
    public BizChangeLogDO buildLogWhenPublishDateChange(String oldValue, String newValue, Long id, Integer type) {
        BizChangeLogDO logDO = newBizChangeLogDO(false, type);
        logDO.setMainId(id);
        logDO.setField(BizChangeLogFieldEnum.PROJECT_RELEASE_DATE.getText());
        logDO.setOldValue(oldValue);
        logDO.setNewValue(newValue);
        return logDO;
    }

    @Override
    public BizChangeLogDO buildLogWhenUpdateFiles(String oldValue, String newValue, Long id, String action) {
        BizChangeLogDO logDO = newBizChangeLogDO(true, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
        logDO.setMainId(id);
        logDO.setField(BizChangeLogFieldEnum.ATTACHMENT.getText());
        logDO.setOldValue(oldValue);
        logDO.setNewValue(newValue);
        logDO.setAction(action);
        return logDO;
    }

    private BizChangeLogDO newBizChangeLogDO(Boolean isUser, Integer type) {
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

    @Override
    public void updateReceiveMan(Long bizDemandId, String oldReceiveMan, String newReceiveMan) {
        if (Objects.equals(oldReceiveMan, newReceiveMan)) {
            return;
        }
        addLogWhenModifyData(oldReceiveMan, newReceiveMan, bizDemandId, BizChangeLogFieldEnum.RECEIVE_MAN.getText(), true);
    }
}
