package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class BizLabelComponentImpl implements BizLabelComponent {


    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private BizLabelMapper bizLabelMapper;

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private BugLogMapper bugLogMapper;

    @Override
    public void addLog(Long mainId, List<Long> labelIds, Integer type, Boolean add) {
        List<LabelDO> labelDOList = labelMapper.getByIds(labelIds);
        List<String> labelNames = labelDOList.stream().map(LabelDO::getName).collect(Collectors.toList());
        Integer logType = 0;
        if (BizTypeEnum.PROJECT.getCode().equals(type)) {
            logType = BizChangeLogTypeEnum.PROJECT.getCode();
        } else if (BizTypeEnum.PRODUCT_DEMAND.getCode().equals(type)) {
            logType = BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode();
        } else if (BizTypeEnum.BIZ_DEMAND.getCode().equals(type)) {
            logType = BizChangeLogTypeEnum.BIZ_DEMAND.getCode();
        } else if (BizTypeEnum.BUG_OFFLINE.getCode().equals(type)) {
            logType = BugLogTypeEnum.OFFLINE.getCode();
        } else if (BizTypeEnum.BUG_ONLINE.getCode().equals(type)) {
            logType = BugLogTypeEnum.ONLINE.getCode();
        }
        List<BizChangeLogDO> bizChangeLogDOList = new ArrayList<>();
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        for (String labelName : labelNames) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            String createMan = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
            String createManId = userInfo.getId();
            if (BizTypeEnum.BUG_OFFLINE.getCode().equals(type) || BizTypeEnum.BUG_ONLINE.getCode().equals(type)) {
                BugLogDO bugLogDO = new BugLogDO();
                bugLogDO.setType(logType);
                bugLogDO.setMainId(mainId);
                bugLogDO.setField(BugFieldEnum.LABEL.getText());
                bugLogDO.setOldValue(labelName);
                bugLogDO.setNewValue(labelName);
                String action = add ? ButtonActionEnum.ADD.getText() : ButtonActionEnum.DELETE.getText();
                bugLogDO.setAction(action);
                bugLogDO.setCreateMan(createMan);
                bugLogDO.setCreateManId(createManId);
                bugLogDOList.add(bugLogDO);
            } else {
                BizChangeLogDO logDO = new BizChangeLogDO();
                logDO.setType(logType);
                logDO.setMainId(mainId);
                logDO.setField(BizChangeLogFieldEnum.LABEL.getText());
                logDO.setOldValue(labelName);
                logDO.setNewValue(labelName);
                String action = add ? ButtonActionEnum.ADD.getText() : ButtonActionEnum.DELETE.getText();
                logDO.setAction(action);
                logDO.setCreateMan(createMan);
                logDO.setCreateManId(createManId);
                bizChangeLogDOList.add(logDO);
            }
        }
        if (CollectionUtils.isNotEmpty(bizChangeLogDOList)) {
            bizChangeLogMapper.batchInsert(bizChangeLogDOList);
        }
        if (CollectionUtils.isNotEmpty(bugLogDOList)) {
            bugLogMapper.batchInsert(bugLogDOList);
        }
    }

    @Override
    public void addLabel(Long bizId, List<Long> labelIds, Integer type) {
        List<BizLabelDO> bizLabelDOList = labelIds.stream().map(a -> {
            BizLabelDO bizLabelDO = new BizLabelDO();
            bizLabelDO.setBizId(bizId);
            bizLabelDO.setType(type);
            bizLabelDO.setLabelId(a);
            return bizLabelDO;
        }).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(bizLabelDOList)) {
            bizLabelMapper.batchInsert(bizLabelDOList);
        }
    }
}
