package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.ProductDemandMD;
import com.timevale.forward.service.component.ProductDemandLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProductDemandLogComponentImpl implements ProductDemandLogComponent {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    /**
     * 编辑时,记录日志
     *
     * @param oldObj oldObj
     * @param newObj newObj
     */
    @Override
    public void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        //{操作人} 把{字段名称} 从{原内容}改为{最新内容}
        ProductDemandMD oldPdm = ProductDemandCopier.INSTANCE.change(oldObj);
        ProductDemandMD newPdm = ProductDemandCopier.INSTANCE.change(newObj);
        List<BizChangeLogDO> logs = FieldCompareUtil.commonCompare(oldPdm, newPdm, BizChangeLogDO.class);

        //类型
        List<Integer> oldTypes = JSON.parseArray(oldObj.getType(), Integer.class);
        List<Integer> newTypes = JSON.parseArray(newObj.getType(), Integer.class);
        if (!CollectionUtil.isEqualList(oldTypes, newTypes)) {
            String oldValue = oldTypes.stream().map(ProductDemandTypeEnum::getTextByCode).collect(Collectors.joining(","));
            String newValue = newTypes.stream().map(ProductDemandTypeEnum::getTextByCode).collect(Collectors.joining(","));
            logs.add(createLog(oldObj.getId(), BizChangeLogFieldEnum.PRODUCT_DEMAND_TYPE.getText(), oldValue, newValue, null));
        }

        //产品线
        if (!Objects.equals(oldObj.getProductLineId(), newObj.getProductLineId())) {
            List<Long> productLineIds = Lists.newArrayList(oldObj.getProductLineId(), newObj.getProductLineId());
            Map<Long, String> productLineMap = productLineMapper.selectByIds(productLineIds)
                    .stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (v1, v2) -> v2));
            String oldValue = productLineMap.get(oldObj.getProductLineId());
            String newValue = productLineMap.get(newObj.getProductLineId());
            logs.add(createLog(oldObj.getId(), BizChangeLogFieldEnum.PRODUCT_LINE.getText(), oldValue, newValue, null));
        }
        logs.forEach(a -> {
            a.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            a.setCreateManId(userInfo.getId());
        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }

    @Override
    public BizChangeLogDO getLog(String oldValue, String newValue, Long id, String field, Boolean active) {
        BizChangeLogDO log = createLog(id, field, oldValue, newValue, "");
        if (active) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            log.setCreateManId(userInfo.getId());
            log.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        } else {
            log.setCreateManId(CommonConstant.SYSTEM);
            log.setCreateMan(CommonConstant.SYSTEM);
        }
        return log;
    }

    @Override
    public void batchAddLog(List<BizChangeLogDO> bizChangeLogDOList) {
        if (CollectionUtil.isEmpty(bizChangeLogDOList)) {
            return;
        }
        bizChangeLogMapper.batchInsert(bizChangeLogDOList);
    }

    /**
     * 项目状态改变时引起的产品需求状态变化
     *
     * @param statusMap statusMap
     * @param newStauts newStauts
     */
    @Override
    public void addLogAsProjectStatusChange(Map<Long, Integer> statusMap, Integer newStauts) {
        //系统 把{xx状态字段名称xx}从{原状态} 改为{新状态}
        List<BizChangeLogDO> logs = new ArrayList<>();
        statusMap.forEach((id, status) -> {
            if (!Objects.equals(status, newStauts)) {
                String oldValue = ProductDemandStatusEnum.getTextByCode(status);
                String newValue = ProductDemandStatusEnum.getTextByCode(newStauts);
                BizChangeLogDO logDO = createLog(id, BizChangeLogFieldEnum.PRODUCT_DEMAND_STATUS.getText(), oldValue, newValue, null);
                logDO.setCreateMan(CommonConstant.SYSTEM);
                logDO.setCreateManId(CommonConstant.SYSTEM);
                logs.add(logDO);
            }
        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }

    /**
     * 按钮点击时引起的产品需求状态变化
     *
     * @param oldStatus oldStatus
     * @param newStatus newStatus
     * @param id        id
     * @param action    action
     */
    @Override
    public void addLogWhenStatusChange(Integer oldStatus, Integer newStatus, Long id, String action) {
        //{操作人}点击 {按钮名称} ,状态改为{操作后状态},
        String oldValue = ProductDemandStatusEnum.getTextByCode(oldStatus);
        String newValue = ProductDemandStatusEnum.getTextByCode(newStatus);
        BizChangeLogDO logDO = createLog(id, BizChangeLogFieldEnum.PRODUCT_DEMAND_STATUS.getText(), oldValue, newValue, action);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        logDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        logDO.setCreateManId(userInfo.getId());
        bizChangeLogMapper.insert(logDO);
    }

    /**
     * 关联/删除关联/作废产品需求时,双向记录日志
     *
     * @param name         name
     * @param id           id
     * @param bdNameMap    bdNameMap
     * @param linkOrUnlink linkOrUnlink
     */
    @Override
    public void addLogWhenLinkOrUnlink(String name, Long id, Map<Long, String> bdNameMap, String linkOrUnlink, BizChangeLogTypeEnum bizChangeLogTypeEnum) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        //为空表示非主动点击删除按钮,赋值SYSTEM-SYSTEM
        boolean empty = StringUtils.isEmpty(linkOrUnlink);
        String action = empty ? ButtonActionEnum.UN_LINK.getText() : linkOrUnlink;
        String createMan = empty ? CommonConstant.SYSTEM : userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        String createManId = empty ? CommonConstant.SYSTEM : userInfo.getId();

        List<BizChangeLogDO> logs = new ArrayList<>();
        bdNameMap.forEach((bId, bName) -> {
            //1.产品需求记录日志:{操作人}添加/删除 {业务需求}:{业务需求A}
            BizChangeLogDO pdLog = new BizChangeLogDO();
            pdLog.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            pdLog.setMainId(id);
            pdLog.setField(bizChangeLogTypeEnum.getText());
            pdLog.setAction(action);
            pdLog.setOldValue(bName);
            pdLog.setNewValue(bName);
            pdLog.setCreateMan(createMan);
            pdLog.setCreateManId(createManId);
            logs.add(pdLog);

            //1.业务需求记录日志:{操作人}添加/删除 {产品需求}:{产品需求A}
            BizChangeLogDO bdLog = new BizChangeLogDO();
            bdLog.setType(bizChangeLogTypeEnum.getCode());
            bdLog.setMainId(bId);
            bdLog.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
            bdLog.setAction(action);
            bdLog.setOldValue(name);
            bdLog.setNewValue(name);
            bdLog.setCreateMan(createMan);
            bdLog.setCreateManId(createManId);
            logs.add(bdLog);

        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }

    @Override
    public void addLogWhenLinkOrUnlink(String name, Long id, Map<Long, String> bdNameMap, String linkOrUnlink) {
        addLogWhenLinkOrUnlink(name, id, bdNameMap, linkOrUnlink, BizChangeLogTypeEnum.BIZ_DEMAND);
    }

    @Override
    public void addLogWhenLinkOrUnlinkCustomDemand(String name, Long id, Map<Long, String> bdNameMap, String linkOrUnlink) {
        addLogWhenLinkOrUnlink(name, id, bdNameMap, linkOrUnlink, BizChangeLogTypeEnum.CUSTOM_DEMAND);
    }

    @Override
    public void addLogWhenLinkOrUnlinkTrackEvent(Long id, List<String> trackEventName, String linkOrUnlink) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String createMan = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        String createManId = userInfo.getId();

        List<BizChangeLogDO> logs = new ArrayList<>();
        trackEventName.forEach(a -> {
            //1.产品需求记录日志:{操作人}添加/删除 {埋点事件}:{埋点事件A}
            BizChangeLogDO pdLog = new BizChangeLogDO();
            pdLog.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            pdLog.setMainId(id);
            pdLog.setField(BizChangeLogFieldEnum.TRACK_EVENT.getText());
            pdLog.setAction(linkOrUnlink);
            pdLog.setOldValue(a);
            pdLog.setNewValue(a);
            pdLog.setCreateMan(createMan);
            pdLog.setCreateManId(createManId);
            logs.add(pdLog);
        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }

    private BizChangeLogDO createLog(Long mainId, String field, String oldValue, String newValue, String action) {
        BizChangeLogDO logDO = new BizChangeLogDO();
        logDO.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
        logDO.setMainId(mainId);
        logDO.setField(field);
        logDO.setOldValue(oldValue);
        logDO.setNewValue(newValue);
        logDO.setAction(action);
        return logDO;
    }
}
