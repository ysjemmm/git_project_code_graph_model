package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BugOnlineListCondition;
import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.dao.BugOnlineBizDemandMapper;
import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.dao.BugOnlineModelMapper;
import com.timevale.forward.dal.dao.BugOnlineProductLineMapper;
import com.timevale.forward.dal.dao.BugStatusOperatorMapper;
import com.timevale.forward.dal.dao.CommentMapper;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.dao.ModelMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOnlineCustomDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.BugOnlineListDO;
import com.timevale.forward.dal.entity.BugOnlineModelDO;
import com.timevale.forward.dal.entity.BugOnlineProductLineDO;
import com.timevale.forward.dal.entity.BugOnlineStatusOperatorDO;
import com.timevale.forward.dal.entity.CommentDO;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.BugOnlineService;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.BugOnlineAcceptanceReq;
import com.timevale.forward.facade.api.request.BugOnlineAddReq;
import com.timevale.forward.facade.api.request.BugOnlineAttachToBizReq;
import com.timevale.forward.facade.api.request.BugOnlineConfirmRepairReq;
import com.timevale.forward.facade.api.request.BugOnlineDetailReq;
import com.timevale.forward.facade.api.request.BugOnlineGetFieldReq;
import com.timevale.forward.facade.api.request.BugOnlineGetReq;
import com.timevale.forward.facade.api.request.BugOnlineIdsReq;
import com.timevale.forward.facade.api.request.BugOnlineModifyReq;
import com.timevale.forward.facade.api.request.BugOnlineNoRepairReq;
import com.timevale.forward.facade.api.request.BugOnlineOnlineReq;
import com.timevale.forward.facade.api.request.BugOnlineOpenAgainReq;
import com.timevale.forward.facade.api.request.BugOnlinePriorityGetReq;
import com.timevale.forward.facade.api.request.BugOnlineRepairFailedReasonReq;
import com.timevale.forward.facade.api.request.BugOnlineRepairFinishedReq;
import com.timevale.forward.facade.api.request.BugOnlineReq;
import com.timevale.forward.facade.api.request.BugOnlineStartRepairReq;
import com.timevale.forward.facade.api.request.BugOnlineToBizApplyReq;
import com.timevale.forward.facade.api.request.BugOnlineTransferReq;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.BugOnlineDetailVO;
import com.timevale.forward.facade.api.result.BugOnlineLinkVO;
import com.timevale.forward.facade.api.result.BugOnlineQueryResultVO;
import com.timevale.forward.facade.api.result.BugOnlineSimpleVO;
import com.timevale.forward.facade.api.result.BugOnlineVO;
import com.timevale.forward.facade.api.result.CommentVO;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.GroupVO;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.PriorityStatisticsVO;
import com.timevale.forward.facade.api.result.ProductLineToFieldVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.model.bo.BusinessBO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.model.enums.BizProductLineTypeEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.BugFieldEnum;
import com.timevale.forward.model.enums.BugLogFieldEnum;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.enums.BugOnlineConvertBizStatusEnum;
import com.timevale.forward.model.enums.BugOnlineEnvEnum;
import com.timevale.forward.model.enums.BugOnlinePriorityEnum;
import com.timevale.forward.model.enums.BugOnlineReasonEnum;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.enums.CommentTypeEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.JobFunctionEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.middle.BugOnlineMD;
import com.timevale.forward.model.middle.BusinessMD;
import com.timevale.forward.model.to.PdLineDomainTO;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.component.BugLogComponent;
import com.timevale.forward.service.component.BugOnlineComponent;
import com.timevale.forward.service.component.BugOnlineCustomComponent;
import com.timevale.forward.service.component.BugOnlineModelComponent;
import com.timevale.forward.service.component.BugOnlineProductLineComponent;
import com.timevale.forward.service.component.BugOnlineStatusOperatorComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.LabelComponent;
import com.timevale.forward.service.component.OutBizDealComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProductLineComponent;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.BugOnlineCopier;
import com.timevale.forward.service.copy.BugOnlineCustomCopier;
import com.timevale.forward.service.copy.CommentCopier;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.integration.SoarClient;
import com.timevale.forward.service.integration.crm.CrmClient;
import com.timevale.forward.service.integration.dock.CrmProjectClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BugOnlineAcceptanceFailMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineAcceptanceMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineAddMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineModifyMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineNoRepairMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineOnlineMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineOpenAgainMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineRejectMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineRepairFailedMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineRepairFinishedMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineResubmitNoRepairMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineResubmitOnlineMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineToBizAgreeEvent;
import com.timevale.forward.service.observer.event.BugOnlineToBizApplyEvent;
import com.timevale.forward.service.observer.event.BugOnlineToBizRejectEvent;
import com.timevale.forward.service.observer.event.BugOnlineTransferMsgEvent;
import com.timevale.forward.service.observer.event.OnlineBugStatusChangeEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.request.AccountRequest;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public static class Car {

    public BaseResult<BugOnlineQueryResultVO<BugOnlineVO>> list(BugOnlineQueryList bugOnlineQueryList) {
        // 用户信息
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BugOnlineListCondition condition = BugOnlineCopier.INSTANCE.convert(bugOnlineQueryList);

        // 标志是否有对应数据
        boolean resultIsEmpty = false;
        // 根据tabs添加不同的效果
        String ascription = bugOnlineQueryList.getAscription();
        if (AscriptionEnum.CURRENT_USER.toString().equals(ascription)) {
            condition.setProposerIdList(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.RECEIVE.toString().equals(ascription)) {
            if (Objects.equals(bugOnlineQueryList.getCurrentOperatorOnly(), false)) {
                condition.setHistoryOperators(Lists.newArrayList(
                        userInfo.getAlias() + "-" + userInfo.getName()));
            } else {
                condition.setOperatorIdList(Lists.newArrayList(userInfo.getId()));
            }
        } else if (AscriptionEnum.COPIER.toString().equals(ascription)) {
            condition.setCopier(userInfo.getId());
        } else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(LocalSessionUtils.getUserInfo().getId(), true);
            if (AscriptionEnum.TEAM_SUBMIT.toString().equals(ascription)) {
                Set<String> createIdSet = Sets.newHashSet(condition.getProposerIdList());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setProposerIdList(teamMemberIdList);
            } else if (AscriptionEnum.TEAM_RECEIVE.toString().equals(ascription)) {
                List<String> operatorIdList = condition.getOperatorIdList();
                Set<String> operatorSet = operatorIdList == null ? Sets.newHashSet() : Sets.newHashSet(operatorIdList);
                if (!operatorSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(operatorSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setOperatorIdList(teamMemberIdList);
            }
        }
        if (resultIsEmpty) {
            BugOnlineQueryResultVO<BugOnlineVO> bugOnlineQueryResultVO = new BugOnlineQueryResultVO<>();
            bugOnlineQueryResultVO.setPriorityStatisticsVOList(getPriorityStatisticsVOList(new HashMap<>()));
            bugOnlineQueryResultVO.setPageQueryResult(ResultUtil.pageEmpty());
            return BaseResult.success(bugOnlineQueryResultVO);
        }

        // 归因和具体原因处理, 如果当前选中阶段，但是没有选择当前阶段的具体原因，就填充当前阶段的全部原因
        List<Integer> reasons = bugOnlineQueryList.getReasons();
        List<Integer> reasonStageList = bugOnlineQueryList.getReasonStageList();
        List<Integer> dismissCauselist = bugOnlineQueryList.getDismissCauseList();
        List<Integer> dismissCauseStageList = bugOnlineQueryList.getDismissCauseStageList();
        if (CollUtil.isNotEmpty(reasonStageList)) {
            if (reasons == null) {
                reasons = new ArrayList<>();
            }
            for (Integer stage : reasonStageList) {
                List<Integer> stageReasons = BugOnlineReasonEnum.getByStage(stage);
                if (!CollUtil.containsAny(stageReasons, reasons)) {
                    reasons.addAll(stageReasons);
                }
            }
        }
        if (CollUtil.isNotEmpty(dismissCauseStageList)) {
            if (dismissCauselist == null) {
                dismissCauselist = new ArrayList<>();
            }
            for (Integer stage : dismissCauseStageList) {
                List<Integer> stageReasons = BugOnlineReasonEnum.getByStage(stage);
                if (!CollUtil.containsAny(stageReasons, dismissCauselist)) {
                    dismissCauselist.addAll(stageReasons);
                }
            }
        }
        condition.setReasons(reasons);
        condition.setDismissCauseList(dismissCauselist);

        //是否打标
        if (CollUtil.isNotEmpty(bugOnlineQueryList.getLabelIds()) || CollUtil.isNotEmpty(bugOnlineQueryList.getLabelCategoryIds())) {
            Boolean containLabel = bugOnlineQueryList.getContainLabel();
            List<Long> newLabelIds = labelComponent.getLabelIds(bugOnlineQueryList.getLabelIds(), bugOnlineQueryList.getLabelCategoryIds());
            if (CollectionUtils.isEmpty(newLabelIds) && containLabel) {
                BugOnlineQueryResultVO<BugOnlineVO> bugOnlineQueryResultVO = new BugOnlineQueryResultVO<>();
                bugOnlineQueryResultVO.setPriorityStatisticsVOList(getPriorityStatisticsVOList(new HashMap<>()));
                bugOnlineQueryResultVO.setPageQueryResult(ResultUtil.pageEmpty());
                return BaseResult.success(bugOnlineQueryResultVO);
            }

            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.BUG_ONLINE.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());
            // 设置包含和不包含
            if (containLabel) {
                if (CollectionUtils.isEmpty(bizIds)) {
                    BugOnlineQueryResultVO<BugOnlineVO> bugOnlineQueryResultVO = new BugOnlineQueryResultVO<>();
                    bugOnlineQueryResultVO.setPriorityStatisticsVOList(getPriorityStatisticsVOList(new HashMap<>()));
                    bugOnlineQueryResultVO.setPageQueryResult(ResultUtil.pageEmpty());

                    return BaseResult.success(bugOnlineQueryResultVO);
                }
                condition.setContainIds(bizIds);
            } else {
                condition.setExclusiveIds(bizIds);
            }
        }

        List<BugOnlineListDO> allBugOnlineDOList = bugOnlineMapper.selectListByCondition(BugOnlineCopier.INSTANCE.convert(condition));
        Map<Integer, List<BugOnlineListDO>> bugOnlineListDOMap = allBugOnlineDOList.stream().collect(Collectors.groupingBy(BugOnlineListDO::getPriority));
        log.info("线上BUG优先级统计：{}", bugOnlineListDOMap);

        List<PriorityStatisticsVO> priorityStatisticsVOList = getPriorityStatisticsVOList(bugOnlineListDOMap);

        // 优先级查询
        List<Integer> conditionSubPriorities = condition.getSubPriorities();
        if (CollUtil.isNotEmpty(conditionSubPriorities)) {
            Set<Integer> resultPrioritySet = priorityStatisticsVOList.stream().map(PriorityStatisticsVO::getPriority).collect(Collectors.toSet());
            List<Integer> queryPriorityList = conditionSubPriorities.stream().filter(resultPrioritySet::contains).collect(Collectors.toList());
            if (CollUtil.isEmpty(queryPriorityList)) {
                BugOnlineQueryResultVO<BugOnlineVO> bugOnlineQueryResultVO = new BugOnlineQueryResultVO<>();
                bugOnlineQueryResultVO.setPriorityStatisticsVOList(priorityStatisticsVOList);
                bugOnlineQueryResultVO.setPageQueryResult(ResultUtil.pageEmpty());
                return BaseResult.success(bugOnlineQueryResultVO);
            } else {
                condition.setPriorities(queryPriorityList);
            }
        }

        // 开始分页fff
        String collation = sqlOrderComponent.build(bugOnlineQueryList.getOrderFiled(), bugOnlineQueryList.getOrderCollation());
        PageHelper.startPage(bugOnlineQueryList.pageNum, bugOnlineQueryList.pageSize, collation);

        // 查询并转换
        List<BugOnlineListDO> bugOnlineDOList = bugOnlineMapper.selectListByCondition(condition);
        List<BugOnlineVO> bugOnlineVOList = bugOnlineDOList.stream().map(BugOnlineCopier.INSTANCE::convert).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(bugOnlineVOList)) {
            BugOnlineQueryResultVO<BugOnlineVO> bugOnlineQueryResultVO = new BugOnlineQueryResultVO<>();
            bugOnlineQueryResultVO.setPriorityStatisticsVOList(priorityStatisticsVOList);
            bugOnlineQueryResultVO.setPageQueryResult(ResultUtil.pageEmpty());

            return BaseResult.success(bugOnlineQueryResultVO);
        }

        //标签
        List<Long> bugOnlineIds = bugOnlineDOList.stream().map(BugOnlineListDO::getId).collect(Collectors.toList());

        Map<Long, List<BizLabelSimpleVO>> bizLabelMap = bizLabelComponent.getBizLabelMap(bugOnlineIds, BizTypeEnum.BUG_ONLINE.getCode());

        // 查询对应产品线和业务域
        List<Long> bugOnlineIdList = bugOnlineVOList.stream().map(BugOnlineVO::getId).collect(Collectors.toList());
        List<BugOnlineProductLineDO> bugOnlineProductLineDOList = bugOnlineProductLineMapper.getByBugOnlineIdList(bugOnlineIdList, BizProductLineTypeEnum.BUG_ONLINE.getCode());


        List<Long> productLineIdList = bugOnlineProductLineDOList.stream().map(BugOnlineProductLineDO::getProductLineId).collect(Collectors.toList());
        List<ProductLineDO> productLineDOList = productLineMapper.getByIds(productLineIdList);

        List<Long> bizDomainIdList = productLineDOList.stream().map(ProductLineDO::getBizDomainId).collect(Collectors.toList());
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.getByIds(bizDomainIdList);

        List<BugOnlineModelDO> bugOnlineModelDOList = bugOnlineModelMapper.selectByBugOnlineIdList(bugOnlineIdList);
        List<Long> modelIdList = bugOnlineModelDOList.stream().map(BugOnlineModelDO::getModelId).collect(Collectors.toList());
        List<ModelDO> modelDOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(modelIdList)) {
            modelDOList = modelMapper.getByIds(modelIdList);
        }

        Map<Long, ProductLineDO> productLineMap = productLineDOList.stream().collect(Collectors.toMap(ProductLineDO::getId, Function.identity()));
        Map<Long, BizDomainDO> bizDomainDOMap = bizDomainDOList.stream().collect(Collectors.toMap(BizDomainDO::getId, Function.identity()));
        Map<Long, List<BugOnlineProductLineDO>> bugOnlineProductLineMap =
                bugOnlineProductLineDOList.stream().collect(Collectors.groupingBy(BugOnlineProductLineDO::getBugOnlineId));

        Map<Long, List<BugOnlineModelDO>> bugOnlineModelMap =
                bugOnlineModelDOList.stream().collect(Collectors.groupingBy(BugOnlineModelDO::getBugOnlineId));
        Map<Long, String> modelNameMap = modelDOList.stream().collect(Collectors.toMap(ModelDO::getId, ModelDO::getName, (v1, v2) -> v2));

        Date now = new Date();
        for (BugOnlineVO e : bugOnlineVOList) {
            // 关联的产品线id
            List<Long> eProductLineIdList = bugOnlineProductLineMap.get(e.getId())
                    .stream()
                    .map(BugOnlineProductLineDO::getProductLineId)
                    .collect(Collectors.toList());

            // 关联的产品线
            List<ProductLineDO> eProductLineDOList = eProductLineIdList
                    .stream()
                    .map(productLineMap::get)
                    .collect(Collectors.toList());

            // 关联的产品线名称
            List<String> eProductLineNameList = eProductLineDOList
                    .stream()
                    .map(ProductLineDO::getName)
                    .distinct()
                    .collect(Collectors.toList());

            // 关联的业务域
            List<Long> eBizDomainIdList = eProductLineDOList
                    .stream()
                    .map(ProductLineDO::getBizDomainId)
                    .collect(Collectors.toList());

            // 关联的业务域名称
            List<String> eBizDomainNameList = eBizDomainIdList
                    .stream()
                    .map(i -> bizDomainDOMap.get(i).getName())
                    .distinct()
                    .collect(Collectors.toList());

            // 关联的模块id
            if (bugOnlineModelMap.containsKey(e.getId())) {
                List<Long> eModelIdList = bugOnlineModelMap.get(e.getId())
                        .stream()
                        .map(BugOnlineModelDO::getModelId)
                        .collect(Collectors.toList());
                List<String> modelNames = eModelIdList.stream().filter(modelNameMap::containsKey).map(modelNameMap::get).collect(Collectors.toList());
                e.setModelNames(modelNames);

            }
            e.setProductLineNameList(eProductLineNameList);
            e.setBizDomainNameList(eBizDomainNameList);
            e.setSlaRemainHours(getSlaRemainHours(e, eProductLineDOList, now));

            List<BizLabelSimpleVO> labelSimpleVOList = bizLabelMap.get(e.getId());
            if (CollectionUtils.isNotEmpty(labelSimpleVOList)) {
                e.setLabelNames(labelSimpleVOList);
            }
        }

        // 返回分页数据
        PageInfo<BugOnlineListDO> pageInfo = new PageInfo<>(bugOnlineDOList);
        PageQueryResult<BugOnlineVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bugOnlineVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        BugOnlineQueryResultVO<BugOnlineVO> bugOnlineQueryResultVO = new BugOnlineQueryResultVO<>();
        bugOnlineQueryResultVO.setPriorityStatisticsVOList(priorityStatisticsVOList);
        bugOnlineQueryResultVO.setPageQueryResult(pageQueryResult);

        return BaseResult.success(bugOnlineQueryResultVO);
    }
}