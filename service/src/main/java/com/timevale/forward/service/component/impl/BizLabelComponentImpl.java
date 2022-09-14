package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.LabelSimpleVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.LabelCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Maps;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    @Resource
    private LabelCategoryMapper labelCategoryMapper;

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

    @Override
    public Map<Long, List<BizLabelSimpleVO>> getBizLabelMap(List<Long> bizIds, Integer type) {
        Map<Long, List<BizLabelSimpleVO>> resultMap = new HashMap<>();

        List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByBizIdInType(bizIds, type);
        Map<Long, List<Long>> labelIdMap = bizLabelDOList.stream()
                .collect(Collectors.groupingBy(BizLabelDO::getBizId,
                        Collectors.mapping(BizLabelDO::getLabelId, Collectors.toList())));

        List<Long> labelIds = bizLabelDOList.stream()
                .map(BizLabelDO::getLabelId)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(labelIds)) {
            List<LabelDO> labelDOList = labelMapper.getByIds(labelIds);
            Map<Long, LabelDO> labelNameMap = labelDOList.stream()
                    .collect(Collectors.toMap(LabelDO::getId, Function.identity()));

            List<Long> labelCategoryIdList = labelDOList.stream()
                    .map(LabelDO::getLabelCategoryId).distinct()
                    .collect(Collectors.toList());
            List<LabelCategoryDO> labelCategoryDOList = labelCategoryMapper.get(labelCategoryIdList);

            Map<Long, List<LabelCategoryDO>> labelCategoryMap = labelCategoryDOList.stream()
                    .collect(Collectors.groupingBy(LabelCategoryDO::getId));

            for (Long bizId : bizIds) {
                List<Long> labelIdList = labelIdMap.get(bizId);
                if (CollectionUtils.isEmpty(labelIdList)) {
                    continue;
                }

                List<LabelDO> labelList = labelIdList.stream()
                        .filter(labelNameMap::containsKey)
                        .map(labelNameMap::get)
                        .collect(Collectors.toList());

                List<BizLabelSimpleVO> simpleVOList = LabelCopier.INSTANCE.convert2BizLabel(labelList);

                if (CollectionUtils.isNotEmpty(labelCategoryDOList)) {
                    for (BizLabelSimpleVO labelSimpleVO : simpleVOList) {
                        List<LabelCategoryDO> list = labelCategoryMap.get(labelSimpleVO.getLabelCategoryId());
                        if (CollectionUtils.isNotEmpty(list)) {
                            LabelCategoryDO labelCategoryDO = list.get(0);

                            labelSimpleVO.setLabelCategoryName(labelCategoryDO.getName());
                        }
                    }
                }

                resultMap.put(bizId, simpleVOList);
            }
        }

        return resultMap;
    }
}
