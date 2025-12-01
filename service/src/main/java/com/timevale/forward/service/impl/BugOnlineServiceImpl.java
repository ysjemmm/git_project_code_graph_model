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

/**
 * @author 望轩
 * @date 2022/3/17 16:59
 */
@Slf4j
@LogPoint
@RestService
@RequiredArgsConstructor
public class BugOnlineServiceImpl implements BugOnlineService {
    private final CrmClient crmClient;
    private final FileMapper fileMapper;
    private final ModelMapper modelMapper;
    private final BugLogMapper bugLogMapper;
    private final PersonMapper personMapper;
    private final CommentMapper commentMapper;
    private final FileComponent fileComponent;
    private final LabelComponent labelComponent;
    private final BizLabelMapper bizLabelMapper;
    private final PersonComponent personComponent;
    private final BugOnlineMapper bugOnlineMapper;
    private final BizDomainMapper bizDomainMapper;
    private final BizDemandMapper bizDemandMapper;
    private final BugLogComponent bugLogComponent;
    private final CrmProjectClient crmProjectClient;
    private final BugOfflineMapper bugOfflineMapper;
    private final SqlOrderComponent sqlOrderComponent;
    private final ProductLineMapper productLineMapper;
    private final BizLabelComponent bizLabelComponent;
    private final SoarClient soarClient;
    private final BugOnlineComponent bugOnlineComponent;
    private final OutBizDealComponent outBizDealComponent;
    private final BugOnlineModelMapper bugOnlineModelMapper;
    private final ProductLineComponent productLineComponent;
    private final MessageEventPublisher messageEventPublisher;
    private final InnerUserPersonClient innerUserPersonClient;
    private final BugOnlineModelComponent bugOnlineModelComponent;
    private final BugStatusOperatorMapper bugStatusOperatorMapper;
    private final BugOnlineBizDemandMapper bugOnlineBizDemandMapper;
    private final BugOnlineCustomComponent bugOnlineCustomComponent;
    private final BugOnlineProductLineMapper bugOnlineProductLineMapper;
    private final BugOnlineProductLineComponent bugOnlineProductLineComponent;
    private final BugOnlineStatusOperatorComponent bugOnlineStatusOperatorComponent;
    private final ApplicationEventPublisher eventPublisher;


    @Value("${business}")
    private String business;
    /**
     * 默认经办人,来自运营支撑提报bug
     */
    @Value("${default.operator:shifeng;释沣-余文杰}")
    private String defaultOperator;

    @Value("#{'${crmDockProductLines:1,2,3}'.split(',')}")
    private Set<Long> crmDockProductLines;


    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd");
    /**
     * 天印产品线id列表
     */
    @Value("#{'${sealProductLines:1,2,27,28,30,31,36,70}'.split(',')}")
    private Set<Long> sealProductLines;

    @Value("${infoDomainId:14}")
    private long infoDomainId;

    private static final Set<Integer> sealSlaStatuses =
            Sets.newHashSet(BugOnlineStatusEnum.PROBLEM_REPORT.getCode(),
                    BugOnlineStatusEnum.START_RESPONSE.getCode(),
                    BugOnlineStatusEnum.QUESTION_CONFIRM.getCode(),
                    BugOnlineStatusEnum.QUESTION_REPAIR.getCode(),
                    BugOnlineStatusEnum.REPAIR_CONFIRM.getCode(),
                    BugOnlineStatusEnum.HANG_UP.getCode());

    private static final Set<Integer> nonsealSlaStatuses =
            Sets.newHashSet(BugOnlineStatusEnum.PROBLEM_REPORT.getCode(),
                    BugOnlineStatusEnum.START_RESPONSE.getCode(),
                    BugOnlineStatusEnum.QUESTION_CONFIRM.getCode(),
                    BugOnlineStatusEnum.QUESTION_REPAIR.getCode(),
                    BugOnlineStatusEnum.REPAIR_CONFIRM.getCode(),
                    BugOnlineStatusEnum.HANG_UP.getCode(),
                    BugOnlineStatusEnum.ONLINE.getCode());
    /**
     * SLA剩余时间计算规则：
     * 按照1天24小时计算，0.5天为12小时。
     * 天印： 紧急：1.0天   高：1.5天  中：2.5天  低：5.0天
     * 公有云及其他产品线：紧急：0.5天    高：1天    中：2天   低：5.0天
     */
    private static final Map<String, BigDecimal> sealRemainHoursMap = new HashMap<>();
    private static final Map<String, BigDecimal> nonsealRemainHoursMap = new HashMap<>();

    static {
        sealRemainHoursMap.put("紧急", new BigDecimal(24));
        sealRemainHoursMap.put("高", new BigDecimal(36));
        sealRemainHoursMap.put("中", new BigDecimal(60));
        sealRemainHoursMap.put("低", new BigDecimal(120));
        nonsealRemainHoursMap.put("紧急", new BigDecimal(12));
        nonsealRemainHoursMap.put("高", new BigDecimal(24));
        nonsealRemainHoursMap.put("中", new BigDecimal(84));
        nonsealRemainHoursMap.put("低", new BigDecimal(144));
    }

    @Override
    public BusinessResult<ProductLineToFieldVO> getAllDisplayField(BugOnlineGetFieldReq bugOnlineGetFieldReq) {
        List<BusinessBO> businessBeanList = JSON.parseArray(business, BusinessBO.class);
        List<Long> productLineIdList = bugOnlineGetFieldReq.getProductLineIdList();
        Map<Integer, String> fieldMap = getFieldMap();
        ProductLineToFieldVO productLineToFieldVO = new ProductLineToFieldVO();
        List<String> fieldList = new ArrayList<>();
        productLineIdList.forEach(productLineId -> {
            for (int i = 0; i < businessBeanList.size(); i++) {
                BusinessBO businessBO = businessBeanList.get(i);
                if (businessBO.getFieldValue().contains(productLineId)) {
                    fieldList.add(fieldMap.get(i));
                }
            }
        });

        //属性字段集合去重
        List<String> distinctFieldList = fieldList.stream().distinct().collect(Collectors.toList());

        BusinessResult<ProductLineToFieldVO> businessResult = new BusinessResult<>();
        productLineToFieldVO.setField(distinctFieldList);
        businessResult.setData(productLineToFieldVO);
        return businessResult;
    }

    //获取下标与字段属性之间的映射关系
    public Map<Integer, String> getFieldMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "flowId");
        map.put(1, "mainOId");
        map.put(2, "templateId");
        map.put(3, "appId");
        map.put(4, "sealId");
        map.put(5, "operatorNameAccount");
        map.put(6, "loginAccount");
        return map;
    }

    @Override
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
                Set<String> operatorSet = Sets.newHashSet(condition.getOperatorIdList());
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

    private BigDecimal getSlaRemainHours(BugOnlineVO e, List<ProductLineDO> productLineDOList, Date endDate) {
        Set<Long> bizDomainIds = productLineDOList.stream().map(ProductLineDO::getBizDomainId).collect(Collectors.toSet());
        if (bizDomainIds.size() == 1 && productLineDOList.get(0).getBizDomainId().equals(infoDomainId)) {
            // 仅有数智化中心的情况，不计算
            return null;
        }
        Optional<ProductLineDO> nonSealProductLine = productLineDOList.stream()
                // 去除数智化中心的数据和天印产品线的数据
                .filter(pl -> !Objects.equals(pl.getBizDomainId(), infoDomainId) &&
                        !sealProductLines.contains(pl.getId())).findFirst();
        boolean onlySeal = !nonSealProductLine.isPresent();
        BigDecimal totalRemain;
        if (onlySeal) {
            // 仅有天印产品线时
            if (!sealSlaStatuses.contains(e.getStatus())) {
                return null;
            }
            totalRemain = sealRemainHoursMap.get(e.getPriorityName());
        } else {
            // 包含非天印产品线时
            if (!nonsealSlaStatuses.contains(e.getStatus())) {
                return null;
            }
            totalRemain = nonsealRemainHoursMap.get(e.getPriorityName());
        }
        if (totalRemain == null) {
            return null;
        }
        Date startDate = e.getCreateDate();
        List<String> holidays = soarClient.getHolidays(e.getCreateDate(), endDate);
        if (holidays.contains(DATE_FORMAT.format(startDate))) {
            startDate = DateUtil.getStartOfDay(startDate);
        }
        if (holidays.contains(DATE_FORMAT.format(endDate))) {
            endDate = DateUtil.getStartOfNextDay(endDate);
        }
        // 计算工作日和节假日的差值
        long elapsedMillis = endDate.getTime() - startDate.getTime() - ((long) holidays.size()) * 24 * 60 * 60 * 1000;
        BigDecimal elapsedHours = new BigDecimal(elapsedMillis)
                .divide(new BigDecimal(1000 * 60 * 60), 1, RoundingMode.HALF_UP);
        return totalRemain.subtract(elapsedHours);
    }

    private List<PriorityStatisticsVO> getPriorityStatisticsVOList(Map<Integer, List<BugOnlineListDO>> bugOnlineListDOMap) {
        List<PriorityStatisticsVO> priorityStatisticsVOList = new ArrayList<>();

        PriorityStatisticsVO low = new PriorityStatisticsVO();
        low.setPriority(0);
        List<BugOnlineListDO> lowList = bugOnlineListDOMap.get(0);
        if (CollectionUtils.isNotEmpty(lowList)) {
            low.setCount(lowList.size());
        } else {
            low.setCount(0);
        }

        PriorityStatisticsVO middle = new PriorityStatisticsVO();
        middle.setPriority(1);
        List<BugOnlineListDO> middleList = bugOnlineListDOMap.get(1);
        if (CollectionUtils.isNotEmpty(middleList)) {
            middle.setCount(middleList.size());
        } else {
            middle.setCount(0);
        }

        PriorityStatisticsVO high = new PriorityStatisticsVO();
        high.setPriority(2);
        List<BugOnlineListDO> highList = bugOnlineListDOMap.get(2);
        if (CollectionUtils.isNotEmpty(highList)) {
            high.setCount(highList.size());
        } else {
            high.setCount(0);
        }

        PriorityStatisticsVO emergent = new PriorityStatisticsVO();
        emergent.setPriority(3);
        List<BugOnlineListDO> emergentList = bugOnlineListDOMap.get(3);
        if (CollectionUtils.isNotEmpty(emergentList)) {
            emergent.setCount(emergentList.size());
        } else {
            emergent.setCount(0);
        }

        priorityStatisticsVOList.add(emergent);
        priorityStatisticsVOList.add(high);
        priorityStatisticsVOList.add(middle);
        priorityStatisticsVOList.add(low);

        return priorityStatisticsVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BugOnlineAddReq addReq) {
        AssertUtil.checkState(!addReq.getName().contains(CommonConstant.BLANK), "线上bug名称中请勿包含空格");

        // 校验天印的产品线，需要AppId 和 详细版本号
        if (BugOnlineEnvEnum.PRODUCE_ENV.getCode().equals(addReq.getEnv())) {
            Collection<Long> intersection = CollUtil.intersection(crmDockProductLines, addReq.getProductLineIdList());
            if (CollUtil.isNotEmpty(intersection)) {
                Optional<String> appIdOptional = Optional.ofNullable(addReq.getBusiness())
                        .map(JSONObject::parseObject)
                        .map(e -> e.getString("appId"))
                        .filter(StrUtil::isNotEmpty);
                if (!appIdOptional.isPresent() || StrUtil.isEmpty(addReq.getDetailVersionId())) {
                    throw new BaseBizRuntimeException("请先在项目的【项目概览-客户运维信息】中维护好APPID和产品版本后再提交此产品线的BUG");
                }
            }
        }

        if (Objects.equals(addReq.getSource(), "support")) {
            log.info("默认经办人:{}", defaultOperator);
            String[] defaultOperators = defaultOperator.split(";");
            addReq.setOperatorId(defaultOperators[0]);
            addReq.setOperator(defaultOperators[1]);
        }
        if (StringUtils.isNotBlank(addReq.getBizId())) {
            outBizDealComponent.checkBizIdExistence(addReq.getBizId());
        }

        if (StrUtil.isNotEmpty(addReq.getCustomerName())) {
            String postGrade = crmClient.getPostGrade(addReq.getCustomerName());
            addReq.setCustomerGrade(postGrade);
        }

        // 如果有客户名称但是没有客户等级则尝试填入
        if (StrUtil.isNotEmpty(addReq.getCustomerName())) {
            Optional.ofNullable(crmClient.getPostGrade(addReq.getCustomerName()))
                    .ifPresent(addReq::setCustomerGrade);
        }

        // 兜底计算优先级
        if (addReq.getPriority() == null || BooleanUtil.isFalse(addReq.getFixedPriority())) {
            BugOnlinePriorityGetReq priorityGetReq = BugOnlineCopier.INSTANCE
                    .do2req(BugOnlineCopier.INSTANCE.req2do(addReq), addReq.getProductLineIdList());
            Integer priority = bugOnlineComponent.calculatePriority(priorityGetReq);
            addReq.setPriority(priority);
        }

        // 加急bug开关
        if (BooleanUtil.isTrue(addReq.getIsUrgent())) {
            if (StrUtil.isBlank(addReq.getUrgentDescription())) {
                throw new BaseBizRuntimeException("bug标记为加急时必须指定原因");
            }
            addReq.setPriority(BugOnlinePriorityEnum.URGENT.getCode());
        } else {
            addReq.setUrgentDescription(null);
            addReq.setUrgentFiles(Collections.emptyList());
        }

        // req 转换为 do
        BugOnlineDO bugOnlineDO = BugOnlineCopier.INSTANCE.req2do(addReq);

        if (StringUtils.isNotEmpty(bugOnlineDO.getDynamicFormFields())) {
            AssertUtil.checkState(bugOnlineDO.getDynamicFormFields().length() <= 1000, "动态表单数据总长度不能超过1000字符");
        }
        // 线上bug落库
        bugOnlineMapper.insert(bugOnlineDO);

        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, null, BugOnlineStatusEnum.PROBLEM_REPORT.getText(), bugOnlineDO.getModifyMan()));

        // 关联产品线
        bugOnlineProductLineComponent.add(addReq.getProductLineIdList(), bugOnlineDO.getId(), BizProductLineTypeEnum.BUG_ONLINE.getCode());
        // 关联模块
        bugOnlineModelComponent.add(addReq.getModelIds(), bugOnlineDO.getId());

        // 附件数据
        List<FileAddReq> files = addReq.getFiles();
        if (CollectionUtils.isNotEmpty(files)) {
            fileComponent.add(files, bugOnlineDO.getId(), FileTypeEnum.BUG_ONLINE.getCode());
        }

        // 加急bug附件数据
        List<FileAddReq> urgentFiles = addReq.getUrgentFiles();
        if (BooleanUtil.isTrue(addReq.getIsUrgent()) && CollectionUtils.isNotEmpty(urgentFiles)) {
            fileComponent.add(urgentFiles, bugOnlineDO.getId(), FileTypeEnum.URGENT_ONLINE_BUG.getCode());
        }

        // 抄送人数据
        List<PersonAddReq> recipients = addReq.getRecipients();
        if (CollectionUtils.isNotEmpty(recipients)) {
            personComponent.add(recipients, bugOnlineDO.getId(), PersonTypeEnum.BUG_ONLINE_CC.getCode());
        }

        // 责任人
        List<PersonAddReq> principalList = addReq.getPrincipalList();
        if (CollUtil.isNotEmpty(principalList)) {
            personComponent.add(principalList, bugOnlineDO.getId(), PersonTypeEnum.BUG_ONLINE_PRINCIPAL.getCode());
        }

        // 客户信息
        if (CollectionUtils.isNotEmpty(addReq.getCustomList())) {
            bugOnlineCustomComponent.add(addReq.getCustomList(), bugOnlineDO.getId());
        }

        //bug日志表记录一条新增数据
        bugLogComponent.bugOnlineInit(bugOnlineDO.getId());
        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        //标签
        if (CollectionUtils.isNotEmpty(addReq.getLabelIds())) {
            bizLabelComponent.addLabel(bugOnlineDO.getId(), addReq.getLabelIds(), BizTypeEnum.BUG_ONLINE.getCode());
            bizLabelComponent.addLog(bugOnlineDO.getId(), addReq.getLabelIds(), BizTypeEnum.BUG_ONLINE.getCode(), true);
        }

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineAddMsgEvent(
                        this,
                        bugOnlineDO.getName(),
                        BugOnlinePriorityEnum.getTextByCode(bugOnlineDO.getPriority()),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );

        if (StringUtils.isNotBlank(addReq.getBizId())) {
            outBizDealComponent.sendBugOnlineRelMsg(bugOnlineDO);
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> delete(BugOnlineReq deleteReq) {
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(deleteReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //删除线上bug
        bugOnlineMapper.delete(deleteReq.getId());

        //删除bug日志表中的数据
        bugLogMapper.deleteByBugId(deleteReq.getId(), BugLogTypeEnum.ONLINE.getCode());

        //删除线上bug产品线映射表里面的数据
        BugOnlineProductLineDO bugOnlineProductLineDO = new BugOnlineProductLineDO();
        bugOnlineProductLineDO.setBugOnlineId(deleteReq.getId());
        bugOnlineProductLineDO.setType(BizProductLineTypeEnum.BUG_ONLINE.getCode());
        bugOnlineProductLineDO.setIsDeleted(true);
        bugOnlineProductLineMapper.update(bugOnlineProductLineDO);

        // 删除线上bug和客户关联
        bugOnlineCustomComponent.delete(deleteReq.getId());

        // 删除抄送人、责任人
        personComponent.update(Collections.emptyList(), deleteReq.getId(), PersonTypeEnum.BUG_ONLINE_CC.getCode());
        personComponent.update(Collections.emptyList(), deleteReq.getId(), PersonTypeEnum.BUG_ONLINE_PRINCIPAL.getCode());

        //删除评论数据
        commentMapper.delete(deleteReq.getId(), CommentTypeEnum.BUG_ONLINE.getCode());

        //删除附件数据
        fileComponent.update(Collections.emptyList(), deleteReq.getId(), FileTypeEnum.BUG_ONLINE.getCode());

        //删除加急附件数据
        fileComponent.update(Collections.emptyList(), deleteReq.getId(), FileTypeEnum.URGENT_ONLINE_BUG.getCode());

        //查询所有的状态变更id
        List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(bugOnlineDO.getId(), BugLogTypeEnum.ONLINE.getCode(), true);
        List<Long> bugLogStatusIdList = bugLogDOList.stream().map(BugLogDO::getId).collect(Collectors.toList());

        //删除bug状态人员处理表里面的数据
        if (CollectionUtils.isNotEmpty(bugLogStatusIdList)) {
            bugStatusOperatorMapper.deleteByBugLogId(bugLogStatusIdList);
        }
        deleteLinkBug(bugOnlineDO.getId(), bugOnlineDO.getLinkBugId(), true);
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);

        bizLabelComponent.deleteLabel(deleteReq.getId(), BizTypeEnum.BUG_ONLINE.getCode());

        bugOnlineStatusOperatorComponent.delete(deleteReq.getId());
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<String> modify(BugOnlineModifyReq modifyReq) {
        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(modifyReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        List<BugLogDO> checkBugLogList = bugLogMapper.selectByBugOfflineIdAndType(bugOnlineDO.getId(), BugLogTypeEnum.ONLINE.getCode(), false);
        if (CollectionUtils.isNotEmpty(checkBugLogList)) {
            checkBugLogList.sort(Comparator.comparing(BugLogDO::getCreateDate).reversed());
            if (!Objects.equals(modifyReq.getBugLogLastCreateDate(), checkBugLogList.get(0).getCreateDate())) {
                throw new BaseBizRuntimeException("当前页面数据发生变化,请刷新后重试");
            }
        }

        // 如果有客户名称但是没有客户等级则尝试填入
        if (StrUtil.isNotEmpty(modifyReq.getCustomerName())) {
            Optional.ofNullable(crmClient.getPostGrade(modifyReq.getCustomerName()))
                    .ifPresent(modifyReq::setCustomerGrade);
        }

        // 若是没有固定优先级，则做兜底计算
        if (BooleanUtil.isFalse(modifyReq.getFixedPriority())) {
            BugOnlinePriorityGetReq priorityGetReq = BugOnlineCopier.INSTANCE
                    .do2req(BugOnlineCopier.INSTANCE.change(modifyReq), modifyReq.getProductLineIdList());
            Integer priority = bugOnlineComponent.calculatePriority(priorityGetReq);
            modifyReq.setPriority(priority);
        }

        //老的线上bug比较对象、产品线、模块
        BugOnlineMD oldBugOnlineMD = BugOnlineCopier.INSTANCE.do2md(bugOnlineDO);
        List<Long> oldProductLineIdList = bugOnlineProductLineMapper.selectProductLineIds(modifyReq.getId(), BizProductLineTypeEnum.BUG_ONLINE.getCode());
        List<Long> oldModelList = bugOnlineModelMapper.selectModelIds(modifyReq.getId());

        // 加急
        if (BooleanUtil.isTrue(modifyReq.getIsUrgent())) {
            if (StrUtil.isBlank(modifyReq.getUrgentDescription())) {
                throw new BaseBizRuntimeException("bug标记为加急时必须指定原因");
            }
            modifyReq.setPriority(BugOnlinePriorityEnum.URGENT.getCode());
            modifyReq.setPriorityChangeReason("bug加急自动更新为P0");
        }
        else {
            modifyReq.setUrgentDescription(null);
            modifyReq.setUrgentFiles(Collections.emptyList());
        }

        //更新线上bug
        if (StringUtils.isNotEmpty(bugOnlineDO.getDynamicFormFields())) {
            AssertUtil.checkState(bugOnlineDO.getDynamicFormFields().length() <= 1000, "动态表单数据总长度不能超过1000字符");
        }
        BugOnlineDO bugOnlineConvert = BugOnlineCopier.INSTANCE.change(modifyReq);

        bugOnlineMapper.update(bugOnlineConvert);

        //更新附件表
        List<FileAddReq> files = modifyReq.getFiles();
        fileComponent.update(files, modifyReq.getId(), FileTypeEnum.BUG_ONLINE.getCode());

        //更新加急bug附件表
        List<FileAddReq> urgentFiles = CollUtil.defaultIfEmpty(modifyReq.getUrgentFiles(), Collections.emptyList());
        fileComponent.update(urgentFiles, modifyReq.getId(), FileTypeEnum.URGENT_ONLINE_BUG.getCode());

        // 更新客户信息
        bugOnlineCustomComponent.update(modifyReq.getCustomList(), modifyReq.getId());

        //更新抄送人、责任人
        List<PersonAddReq> recipients = modifyReq.getRecipients();
        List<PersonAddReq> principalList = modifyReq.getPrincipalList();
        personComponent.update(recipients, modifyReq.getId(), PersonTypeEnum.BUG_ONLINE_CC.getCode());
        personComponent.update(principalList, modifyReq.getId(), PersonTypeEnum.BUG_ONLINE_PRINCIPAL.getCode());

        // 更新产品线和模块
        bugOnlineProductLineComponent.update(modifyReq.getProductLineIdList(), modifyReq.getId(), BizProductLineTypeEnum.BUG_ONLINE.getCode());
        bugOnlineModelComponent.update(modifyReq.getModelIds(), modifyReq.getId());

        //新的线上bug比较对象
        BugOnlineMD newBugOnlineMD = BugOnlineCopier.INSTANCE.req2md(modifyReq);
        BugOnlineDO newBugOnlineDO = BugOnlineCopier.INSTANCE.change(modifyReq);

        //比较编辑修改的一般字段，生成结果集合
        List<BugLogDO> bugLogDOList = FieldCompareUtil.commonCompare(oldBugOnlineMD, newBugOnlineMD, BugLogDO.class);
        //模块日志
        bugLogDOList.addAll(compareExtField(bugOnlineDO, newBugOnlineDO));
        bugLogDOList.addAll(compareModel(oldModelList, modifyReq.getModelIds(), bugOnlineDO.getId()));
        bugLogDOList.addAll(compareProductLine(oldProductLineIdList, modifyReq.getProductLineIdList(), bugOnlineDO.getId()));
        bugLogDOList.addAll(bugOnlineComponent.compareBugOffline(bugOnlineDO.getId(), bugOnlineDO.getBugOfflineId(), modifyReq.getBugOfflineId()));
        bugLogComponent.add(bugLogDOList);

        // 交付项目变更日志
        bugLogComponent.customDevProject(bugOnlineDO.getId(), bugOnlineDO.getSourceId(), modifyReq.getSourceId());

        //如果经办人变了，但是状态没有变化，需要往状态人员处理表中插入一条数据，并且需要发送钉钉消息
        if (!bugOnlineDO.getOperatorId().equals(newBugOnlineDO.getOperatorId())) {
            //往bug状态人员处理表中插入一条记录
            bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), newBugOnlineDO.getOperatorId(), newBugOnlineDO.getOperator());

            //发送钉钉消息
            messageEventPublisher.publish(
                    new BugOnlineModifyMsgEvent(
                            this,
                            modifyReq.getName(),
                            BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()),
                            modifyReq.getOperatorId(),
                            bugOnlineDO.getId(),
                            BugOnlinePriorityEnum.getTextByCode(modifyReq.getPriority())
                    )
            );
        }

        String tips = updateLinkBug(modifyReq.getId(), modifyReq.getLinkBugId());
        return BaseResult.success(tips);
    }

    @Override
    public BusinessResult<BugOnlineDetailVO> get(BugOnlineDetailReq getReq) {
        //查询线上bug
        final Long bugOnlineId = getReq.getId();
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineId);
        AssertUtil.notNull(bugOnlineDO, "该线上bug不存在");

        //BugOnlineDO --> BugOnlineDetailVO
        BugOnlineDetailVO bugOnlineDetailVO = BugOnlineCopier.INSTANCE.convert(bugOnlineDO);

        // 群组信息
        GroupVO groupVO = new GroupVO();
        groupVO.setGroupId(bugOnlineDO.getGroupId());
        groupVO.setGroupName(bugOnlineDO.getGroupName());
        bugOnlineDetailVO.setGroup(groupVO);

        crmProjectClient.getProject(bugOnlineDO.getSourceId())
                .ifPresent(p -> bugOnlineDetailVO.setCustomerDevProjectName(p.getProjectName()));

        //通过线上bug和产品线映射表查询所有的产品线id
        List<Long> productLineIdList = bugOnlineProductLineMapper.selectProductLineIds(bugOnlineId, BizProductLineTypeEnum.BUG_ONLINE.getCode());

        //如果产品线id不为空，批量查询产品线并进行类型转换
        if (CollectionUtils.isNotEmpty(productLineIdList)) {
            //批量查询产品线
            List<ProductLineDO> productLineDOList = productLineMapper.getByIds(productLineIdList);
            //转换 ProductLineDO --> ProductLineVO
            List<ProductLineVO> productLineVOList = productLineDOList.stream().map(ProductLineCopier.INSTANCE::convert).collect(Collectors.toList());
            //产品线信息存储到详情参数里面
            bugOnlineDetailVO.setProductLineVOList(productLineVOList);
        }

        //如果线上bug转化了业务需求，则查询并转化业务需求
        List<Long> bizDemandIds = bugOnlineBizDemandMapper.getBizDemandIds(bugOnlineId);
        if (CollectionUtils.isNotEmpty(bizDemandIds)) {
            List<BizDemandDO> bizDemands = bizDemandMapper.getByIds(bizDemandIds);
            List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.transfer(bizDemands);
            bugOnlineDetailVO.setBizDemands(bizDemandVOList);
        }

        ///查询附件
        Map<Integer, List<FileDO>> type2FileDOList = fileMapper.selectByAttacheIdListAndTypes(bugOnlineId, Lists.newArrayList(FileTypeEnum.BUG_ONLINE.getCode(), FileTypeEnum.URGENT_ONLINE_BUG.getCode()))
                .stream().collect(Collectors.groupingBy(FileDO::getType));
        List<FileDO> normalfileDOList = type2FileDOList.get(FileTypeEnum.BUG_ONLINE.getCode());
        //线上bug附件
        if (CollectionUtils.isNotEmpty(normalfileDOList)) {
            List<FileVO> fileVOList = normalfileDOList.stream().map(FileCopier.INSTANCE::change).collect(Collectors.toList());
            //附件信息存储到详情参数里面
            bugOnlineDetailVO.setFiles(fileVOList);
        }

        //加急bug附件
        List<FileDO> urgentFileDOList = type2FileDOList.get(FileTypeEnum.URGENT_ONLINE_BUG.getCode());
        if (CollectionUtils.isNotEmpty(urgentFileDOList)) {
            List<FileVO> fileVOList = urgentFileDOList.stream().map(FileCopier.INSTANCE::change).collect(Collectors.toList());
            bugOnlineDetailVO.setUrgentFiles(fileVOList);
        }

        //查询抄送人
        List<PersonDO> personDOList = personMapper.select(PersonListCondition.builder()
                .mainId(bugOnlineId)
                .type(PersonTypeEnum.BUG_ONLINE_CC.getCode())
                .build());
        //如果存在抄送人转化类型
        if (CollectionUtils.isNotEmpty(personDOList)) {
            List<PersonVO> personVOList = personDOList.stream()
                    .map(PersonCopier.INSTANCE::change).collect(Collectors.toList());
            //抄送人信息存储到详情参数里面
            bugOnlineDetailVO.setRecipientInfoList(personVOList);
        }

        //查询评论
        List<CommentDO> commentDOList = commentMapper
                .select(bugOnlineId, CommentTypeEnum.BUG_ONLINE.getCode());
        //如果评论表不为空，转化并添加到详情参数中
        if (CollectionUtils.isNotEmpty(commentDOList)) {
            List<CommentVO> commentVOList = commentDOList.stream().map(CommentCopier.INSTANCE::change)
                    .collect(Collectors.toList());
            bugOnlineDetailVO.setCommentVOList(commentVOList);
        }

        //模块名称
        List<Long> modelIdList = bugOnlineModelMapper.selectModelIds(bugOnlineId);
        if (!CollectionUtils.isEmpty(modelIdList)) {
            String modelName = modelMapper.getByIds(modelIdList).stream().map(ModelDO::getName).collect(Collectors.joining(","));
            bugOnlineDetailVO.setModelName(modelName);
            bugOnlineDetailVO.setModelIds(modelIdList);
        }

        // 查询关联的线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.get(bugOnlineDO.getBugOfflineId());
        if (bugOfflineDO != null) {
            bugOnlineDetailVO.setBugOfflineName(bugOfflineDO.getName());
        }

        // 查询的关联的负责人
        List<PersonDO> principal = personComponent.select(bugOnlineId, PersonTypeEnum.BUG_ONLINE_PRINCIPAL.getCode());
        List<PersonVO> principalVOList = PersonCopier.INSTANCE.transform(principal);
        bugOnlineDetailVO.setPrincipalList(principalVOList);

        // 客户信息
        List<BugOnlineCustomDO> customDOList = bugOnlineCustomComponent.selectByBugOnlineId(bugOnlineDO.getId());
        if (CollectionUtils.isNotEmpty(customDOList)) {
            bugOnlineDetailVO.setCustomList(BugOnlineCustomCopier.INSTANCE.convertListToVO(customDOList));
        }

        //关联的bug/被关联的bug
        if (bugOnlineDO.getLinkBugId() != null) {
            BugOnlineDO linkBug = bugOnlineMapper.get(bugOnlineDO.getLinkBugId());
            bugOnlineDetailVO.setLinkBug(new BugOnlineLinkVO(linkBug.getId(), linkBug.getName()));
        } else {
            List<BugOnlineDO> linkedBug = bugOnlineMapper.selectByLinkBugId(bugOnlineDO.getId());
            List<BugOnlineLinkVO> linkedBugs = linkedBug.stream().map(a -> {
                BugOnlineLinkVO o = new BugOnlineLinkVO();
                o.setId(a.getId());
                o.setName(a.getName());
                return o;
            }).collect(Collectors.toList());
            bugOnlineDetailVO.setLinkedBugs(linkedBugs);
        }
        //日志最新时间
        List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(bugOnlineDO.getId(), BugLogTypeEnum.ONLINE.getCode(), false);
        if (CollectionUtils.isNotEmpty(bugLogDOList)) {
            bugLogDOList.sort(Comparator.comparing(BugLogDO::getCreateDate).reversed());
            bugOnlineDetailVO.setBugLogLastCreateDate(bugLogDOList.get(0).getCreateDate());
        }
        BusinessResult<BugOnlineDetailVO> businessResult = new BusinessResult<>();
        businessResult.setData(bugOnlineDetailVO);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> confirm(BugOnlineReq bugOnlineReq) {
        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineReq.getId());
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        AssertUtil.notNull(bugOnlineDO, "线上bug不存在");
        AssertUtil.checkState(bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.START_RESPONSE.getCode()),
                "当前BUG状态为：" + BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()) +
                        "，请刷新页面后再去操作");

        bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
        bugOnlineMapper.update(bugOnlineDO);
        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.QUESTION_CONFIRM.getText(), bugOnlineDO.getModifyMan()));

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.CONFIRM.getText());
        bugLogDO.setOldValue(BugOnlineStatusEnum.START_RESPONSE.getText());
        bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_CONFIRM.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());

        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<String> startRepair(BugOnlineStartRepairReq startRepairReq) {
        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(startRepairReq.getId());
        AssertUtil.notNull(bugOnlineDO, "线上bug不存在");

        //判断当前状态是否为“问题确认”，“挂起”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.HANG_UP.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击开始修复");
        }

        //保存老的状态
        Long oldBugOfflineId = bugOnlineDO.getBugOfflineId();
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        //得到老的对象
        BugOnlineMD oldBugOnlineMD = BugOnlineCopier.INSTANCE.do2md(bugOnlineDO);

        //线上bug表更新
        bugOnlineDO.setReason(startRepairReq.getReason());
        bugOnlineDO.setReasonStage(startRepairReq.getReasonStage());
        bugOnlineDO.setSolveScheme(startRepairReq.getSolveScheme());
        bugOnlineDO.setBugOfflineId(startRepairReq.getBugOfflineId());
        bugOnlineDO.setProblemReason(startRepairReq.getProblemReason());
        bugOnlineDO.setExpectLaunchDate(startRepairReq.getExpectLaunchDate());
        bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_REPAIR.getCode());
        bugOnlineDO.setTemporarySolution(startRepairReq.getTemporarySolution());
        bugOnlineMapper.update(bugOnlineDO);
        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.QUESTION_REPAIR.getText(), bugOnlineDO.getModifyMan()));

        // 状态log
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.START_REPAIR.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_REPAIR.getText());
        bugLogDO.setMainId(startRepairReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        bugLogMapper.insert(bugLogDO);

        // 线下bug log
        bugLogComponent.bugOffline(bugOnlineDO.getId(), oldBugOfflineId, startRepairReq.getBugOfflineId());

        // 比较通用字段
        BugOnlineMD newBugOnlineMD = BugOnlineCopier.INSTANCE.do2md(bugOnlineDO);
        List<BugLogDO> bugLogDOList = FieldCompareUtil.commonCompare(oldBugOnlineMD, newBugOnlineMD, BugLogDO.class);
        bugLogComponent.add(bugLogDOList);

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        String tips = updateLinkBug(startRepairReq.getId(), startRepairReq.getLinkBugId());

        BusinessResult<String> businessResult = new BusinessResult<>();
        businessResult.setData(tips);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> repairFinished(BugOnlineRepairFinishedReq bugOnlineRepairFinishedReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineRepairFinishedReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“问题修复”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_REPAIR.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击修复完毕");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        //保存当前经办人
        String operator = bugOnlineDO.getOperator();
        String operatorId = bugOnlineDO.getOperatorId();

        //保存老的修复失败原因
        String repairFailReason = bugOnlineDO.getRepairFailReason();

        bugOnlineDO.setStatus(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode());
        bugOnlineDO.setRepairFailReason("");
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setOperator(bugOnlineRepairFinishedReq.getOperator());
        bugOnlineDO.setOperatorId(bugOnlineRepairFinishedReq.getOperatorId());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.REPAIR_CONFIRM.getText(), bugOnlineDO.getModifyMan()));

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REPAIR_FINISH.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.REPAIR_CONFIRM.getText());
        bugLogDO.setMainId(bugOnlineRepairFinishedReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //如果此时修复失败原因有值，则需要插入一条bug内容变更记录，因为需要把修复失败原因清空
        if (repairFailReason != null && !repairFailReason.isEmpty()) {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.REPAIR_FAIL_REASON.getText());
            bugLog.setOldValue(repairFailReason);
            bugLog.setMainId(bugOnlineRepairFinishedReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bugLog);
        }

        //如果经办人变了，则添加一条内容变更记录
        if (!operator.equals(bugOnlineDO.getOperator())) {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.OPERATOR.getText());
            bugLog.setOldValue(operator);
            bugLog.setNewValue(bugOnlineDO.getOperator());
            bugLog.setMainId(bugOnlineRepairFinishedReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bugLog);
        }

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineRepairFinishedMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineRepairFinishedReq.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> confirmRepair(BugOnlineConfirmRepairReq confirmRepairReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(confirmRepairReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“QA修复确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击确认修复");
        }

        //校验当前操作人职能是否为测试
        Boolean result = jobFunctionMatch(userInfo.getId(), JobFunctionEnum.QA.getName());
        if (!result) {
            throw new BaseBizRuntimeException("您的职能没有权限点击此按钮");
        }

        //保存老的状态
        Integer oldReason = bugOnlineDO.getReason();
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        Long oldBugOfflineId = bugOnlineDO.getBugOfflineId();

        // 更新线上bug
        bugOnlineDO.setReason(confirmRepairReq.getReason());
        bugOnlineDO.setReasonStage(confirmRepairReq.getReasonStage());
        bugOnlineDO.setBugOfflineId(confirmRepairReq.getBugOfflineId());
        bugOnlineDO.setStatus(BugOnlineStatusEnum.ONLINE.getCode());
        bugOnlineMapper.update(bugOnlineDO);

        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.ONLINE.getText(), bugOnlineDO.getModifyMan()));

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.CONFIRM_REPAIR.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.ONLINE.getText());
        bugLogDO.setMainId(confirmRepairReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        // 线下bug、bug原因的log
        bugLogComponent.bugOffline(confirmRepairReq.getId(), oldBugOfflineId, confirmRepairReq.getBugOfflineId());
        bugLogComponent.reason(confirmRepairReq.getId(), oldReason, confirmRepairReq.getReason());

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> online(BugOnlineOnlineReq onlineReq) {
        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(onlineReq.getId());
        AssertUtil.notNull(bugOnlineDO, "线上bug不存在");

        //保存老的状态
        Integer oldReason = bugOnlineDO.getReason();
        Long oldBugOfflineId = bugOnlineDO.getBugOfflineId();
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        String oldOperator = bugOnlineDO.getOperator();

        //线上bug表更新
        bugOnlineDO.setReason(onlineReq.getReason());
        bugOnlineDO.setReasonStage(onlineReq.getReasonStage());
        bugOnlineDO.setBugOfflineId(onlineReq.getBugOfflineId());
        bugOnlineDO.setStatus(BugOnlineStatusEnum.ACCEPTANCE.getCode());
        //bug状态为待验收时经办人自动转为提交人
        bugOnlineDO.setOperatorId(bugOnlineDO.getProposerId());
        bugOnlineDO.setOperator(bugOnlineDO.getProposer());
        bugOnlineMapper.update(bugOnlineDO);
        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.ACCEPTANCE.getText(), bugOnlineDO.getModifyMan()));

        //往bug日志表中插入一条线上bug状态变更数据
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.ONLINE.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.ACCEPTANCE.getText());
        bugLogDO.setMainId(onlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        bugLogMapper.insert(bugLogDO);

        //新增经办人变更日志
        bugLogComponent.operator(onlineReq.getId(), oldOperator, bugOnlineDO.getOperator(), false);

        // 线下bug, bug原因 log
        bugLogComponent.bugOffline(onlineReq.getId(), oldBugOfflineId, onlineReq.getBugOfflineId());
        bugLogComponent.reason(onlineReq.getId(), oldReason, onlineReq.getReason());

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        new BugOnlineOnlineMsgEvent(
                this,
                bugOnlineDO.getName(),
                bugOnlineDO.getProposerId(),
                bugOnlineDO.getId()
        ).send();

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> openAgain(BugOnlineOpenAgainReq bugOnlineOpenAgainReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineOpenAgainReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“完成”或者“关闭”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.COMPLETE.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.CLOSE.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击重新打开");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        //保存当前经办人和上一阶段经办人
        String operatorId = bugOnlineDO.getOperatorId();
        String operator = bugOnlineDO.getOperator();
        String lastOperator = bugOnlineDO.getLastOperator();
        String lastOperatorId = bugOnlineDO.getLastOperatorId();

        //得到老的对象
        BugOnlineMD oldBugOnlineMD = BugOnlineCopier.INSTANCE.do2md(bugOnlineDO);

        if (bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.CLOSE.getCode())) {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.PROBLEM_REPORT.getCode());
        }
        if (bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.COMPLETE.getCode())) {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
        }
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setOperatorId(lastOperatorId);
        bugOnlineDO.setOperator(lastOperator);
        bugOnlineDO.setDismissCause(null);
        bugOnlineDO.setDismissCauseStage(null);
        bugOnlineDO.setHangUp(false);
        bugOnlineDO.setOpenAgainReason(bugOnlineOpenAgainReq.getOpenAgainReason());
        bugOnlineDO.setOpenCount(bugOnlineDO.getOpenCount() + 1);
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);
        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()), operator));

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.OPEN_AGAIN.getText());
        bugLogDO.setOldValue(oldStatus);
        if (oldStatus.equals(BugOnlineStatusEnum.CLOSE.getText())) {
            bugLogDO.setNewValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        }
        if (oldStatus.equals(BugOnlineStatusEnum.COMPLETE.getText())) {
            bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_CONFIRM.getText());
        }
        bugLogDO.setMainId(bugOnlineOpenAgainReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //得到新的对象
        BugOnlineMD newBugOnlineMD = BugOnlineCopier.INSTANCE.do2md(bugOnlineDO);

        //比较内容是否变化,有变化则插入内容变更记录
        List<BugLogDO> bugLogDOList = FieldCompareUtil.commonCompare(oldBugOnlineMD, newBugOnlineMD, BugLogDO.class);

        if (!CollectionUtils.isEmpty(bugLogDOList)) {
            bugLogMapper.batchInsert(bugLogDOList);
        }

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineOpenAgainMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );
        deleteLinkBug(bugOnlineDO.getId(), bugOnlineDO.getLinkBugId(), false);
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<String> noRepair(BugOnlineNoRepairReq noRepairReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        final Long bugId = noRepairReq.getId();
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(noRepairReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //保存老的状态、经办人、修复失败原因
        String operator = bugOnlineDO.getOperator();
        Integer oldReason = bugOnlineDO.getReason();
        Long oldBugOfflineId = bugOnlineDO.getBugOfflineId();
        String oldRepairFailReason = bugOnlineDO.getRepairFailReason();
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setReason(null);
        bugOnlineDO.setReasonStage(null);
        bugOnlineDO.setBugOfflineId(null);
        bugOnlineDO.setRepairFailReason(null);
        bugOnlineDO.setStatus(BugOnlineStatusEnum.BE_CONFIRM.getCode());
        bugOnlineDO.setLastOperatorId(bugOnlineDO.getOperatorId());
        bugOnlineDO.setLastOperator(bugOnlineDO.getOperator());
        bugOnlineDO.setOperatorId(bugOnlineDO.getProposerId());
        bugOnlineDO.setOperator(bugOnlineDO.getProposer());
        bugOnlineDO.setDismissCause(noRepairReq.getDismissCause());
        bugOnlineDO.setDismissCauseStage(noRepairReq.getDismissCauseStage());
        bugOnlineDO.setTemporarySolution(noRepairReq.getTemporarySolution());

        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);
        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.BE_CONFIRM.getText(), operator));

        // 清空bug原因、关联的线下bug日志
        bugLogComponent.reason(bugId, oldReason, null);
        bugLogComponent.bugOffline(bugId, oldBugOfflineId, null);

        //状态
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        bugLogDOList.add(createBugLog(noRepairReq.getId(), oldStatus, BugOnlineStatusEnum.BE_CONFIRM.getText()
                , ButtonActionEnum.NO_REPAIR.getText(), BugLogFieldEnum.STATUS.getText()));

        //因为新增了驳回原因所以这里需要加入一条内容变更记录
        bugLogDOList.add(createBugLog(noRepairReq.getId(),
                null,
                BugOnlineReasonEnum.getFullTextByCode(bugOnlineDO.getDismissCause()),
                null,
                BugFieldEnum.DISMISS_CAUSE.getText()));

        //如果修复失败原因有值还需要记录一条日志内容记录
        if (StringUtils.isNotEmpty(oldRepairFailReason)) {
            bugLogDOList.add(createBugLog(noRepairReq.getId(), oldRepairFailReason, null
                    , null, BugFieldEnum.REPAIR_FAIL_REASON.getText()));
        }

        //如果经办人变了，则添加一条内容变更记录
        if (!operator.equals(bugOnlineDO.getOperator())) {
            bugLogDOList.add(createBugLog(noRepairReq.getId(), operator, bugOnlineDO.getOperator()
                    , null, BugFieldEnum.OPERATOR.getText()));
        }

        bugLogMapper.batchInsert(bugLogDOList);
        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineNoRepairMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );

        List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByLinkBugId(bugOnlineDO.getId());
        bugOnlineDOList.forEach(a -> messageEventPublisher.publish(
                new BugOnlineResubmitNoRepairMsgEvent(
                        this,
                        bugOnlineDO.getName(),
                        a.getProposerId(),
                        bugOnlineDO.getId()
                )
        ));

        String tips = updateLinkBug(noRepairReq.getId(), noRepairReq.getLinkBugId());

        BusinessResult<String> businessResult = new BusinessResult<>();
        businessResult.setData(tips);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> transfer(BugOnlineTransferReq bugOnlineTransferReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineTransferReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //保存老的经办人
        String oldOperator = bugOnlineDO.getOperator();

        bugOnlineDO.setOperatorId(bugOnlineTransferReq.getUserId());
        bugOnlineDO.setOperator(bugOnlineTransferReq.getUserName());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        //如果不是自己转交给自己
        if (!oldOperator.equals(bugOnlineTransferReq.getUserName())) {
            bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

            //往bug日志表中插入一条线上bug内容变更数据
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.OPERATOR.getText());
            bugLog.setOldValue(oldOperator);
            bugLog.setNewValue(bugOnlineTransferReq.getUserName());
            bugLog.setMainId(bugOnlineTransferReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogMapper.insert(bugLog);

            //发送消息
            messageEventPublisher.publish(
                    new BugOnlineTransferMsgEvent(
                            this,
                            userInfo.getAlias() + "-" + userInfo.getName(),
                            bugOnlineDO.getName(),
                            BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()),
                            bugOnlineTransferReq.getUserId(),
                            bugOnlineDO.getId(),
                            BugOnlinePriorityEnum.getTextByCode(bugOnlineDO.getPriority())
                    )
            );
        }

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> agree(BugOnlineReq bugOnlineReq) {
        //查询线上bug
        final Long bugId = bugOnlineReq.getId();
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugId);
        AssertUtil.notNull(bugOnlineDO, "线上bug不存在");

        //判断当前状态是否为“问题上报”或者“问题确认”状态
        AssertUtil.checkState(bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.BE_CONFIRM.getCode()),
                "当前状态不允许点击同意");

        //判断操作人是否有点击权限
        AssertUtil.checkState(isPermission(bugOnlineDO.getOperatorId()) || isPermission(bugOnlineDO.getProposerId()),
                "您没有点击此按钮的权限");

        // 保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.CLOSE.getCode());
        bugOnlineMapper.update(bugOnlineDO);
        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.CLOSE.getText(), bugOnlineDO.getModifyMan()));

        //往bug日志表中插入一条线上bug状态变更数据
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.AGREE.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.CLOSE.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        bugLogMapper.insert(bugLogDO);

        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        BugOnlineStatusOperatorDO bugOnlineStatusOperatorDO = new BugOnlineStatusOperatorDO();
        bugOnlineStatusOperatorDO.setBugOnlineId(bugOnlineReq.getId());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        bugOnlineStatusOperatorDO.setOperator(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        bugOnlineStatusOperatorDO.setStatus(BugOnlineStatusEnum.CLOSE.getCode());
        bugOnlineStatusOperatorDO.setOperatorId(userInfo.getId());
        bugOnlineStatusOperatorComponent.add(bugOnlineStatusOperatorDO);

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> reject(BugOnlineReq bugOnlineReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“待确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.BE_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击拒绝");
        }

        //判断操作人是否有点击权限
        Boolean operatorResult = isPermission(bugOnlineDO.getOperatorId());
        Boolean proposerResult = isPermission(bugOnlineDO.getProposerId());
        if (!operatorResult && !proposerResult) {
            throw new BaseBizRuntimeException("您没有点击此按钮的权限");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        //保存老的驳回原因
        String oldDismissCause = BugOnlineReasonEnum.getFullTextByCode(bugOnlineDO.getDismissCause());
        //保存老的经办人和老的上一阶段经办人
        String operatorId = bugOnlineDO.getOperatorId();
        String operator = bugOnlineDO.getOperator();
        String lastOperatorId = bugOnlineDO.getLastOperatorId();
        String lastOperator = bugOnlineDO.getLastOperator();

        bugOnlineDO.setStatus(BugOnlineStatusEnum.START_RESPONSE.getCode());
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setOperatorId(lastOperatorId);
        bugOnlineDO.setOperator(lastOperator);
        bugOnlineDO.setDismissCause(null);
        bugOnlineDO.setDismissCauseStage(null);
        bugOnlineDO.setOpenCount(bugOnlineDO.getOpenCount() + 1);
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.START_RESPONSE.getText(), bugOnlineDO.getModifyMan()));

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REFUSED.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.START_RESPONSE.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //因为清空了驳回原因所以这里需要加入一条内容变更记录
        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.DISMISS_CAUSE.getText());
        bugLog.setOldValue(oldDismissCause);
        bugLog.setMainId(bugOnlineReq.getId());
        bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
        //往bug日志表中插入一条线上bug内容变更数据
        bugLogMapper.insert(bugLog);

        //如果经办人变了，则添加一条内容变更记录
        if (!operator.equals(bugOnlineDO.getOperator())) {
            BugLogDO bug = new BugLogDO();
            bug.setField(BugFieldEnum.OPERATOR.getText());
            bug.setOldValue(operator);
            bug.setNewValue(bugOnlineDO.getOperator());
            bug.setMainId(bugOnlineReq.getId());
            bug.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bug);
        }

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineRejectMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );
        deleteLinkBug(bugOnlineDO.getId(), bugOnlineDO.getLinkBugId(), false);
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> reconfirm(BugOnlineReq bugOnlineReq) {
        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        //判断是从哪个状态点击的重新确认按钮
        if (oldStatus.equals(BugOnlineStatusEnum.QUESTION_REPAIR.getText())) {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
        } else {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.START_RESPONSE.getCode());
        }

        bugOnlineDO.setHangUp(false);
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);
        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()), bugOnlineDO.getModifyMan()));

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REPEAT_CONFIRM.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()));
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> temporaryNoRepair(BugOnlineReq bugOnlineReq) {
        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“问题确认”或者“问题修复”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_REPAIR.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击暂不修复");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.HANG_UP.getCode());
        bugOnlineDO.setHangUp(true);
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.HANG_UP.getText(), bugOnlineDO.getModifyMan()));

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.TEMPORARY_NO_REPAIR.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.HANG_UP.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> repairFailed(BugOnlineRepairFailedReasonReq bugOnlineRepairFailedReasonReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineRepairFailedReasonReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“QA修复确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击修复失败");
        }

        //判断操作人是否有点击权限
        Boolean result = jobFunctionMatch(userInfo.getId(), JobFunctionEnum.QA.getName());
        if (!result) {
            throw new BaseBizRuntimeException("您没有点击此按钮的权限");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        //保存老的经办人和老的上一阶段经办人
        String operatorId = bugOnlineDO.getOperatorId();
        String operator = bugOnlineDO.getOperator();
        String lastOperatorId = bugOnlineDO.getLastOperatorId();
        String lastOperator = bugOnlineDO.getLastOperator();

        bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_REPAIR.getCode());
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setOperatorId(lastOperatorId);
        bugOnlineDO.setOperator(lastOperator);
        bugOnlineDO.setRepairFailReason(bugOnlineRepairFailedReasonReq.getRepairFailReason());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.QUESTION_REPAIR.getText(), bugOnlineDO.getModifyMan()));

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REPAIR_FAIL.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_REPAIR.getText());
        bugLogDO.setMainId(bugOnlineRepairFailedReasonReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //因为增加了修复失败原因所以这里需要加入一条内容变更记录
        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.REPAIR_FAIL_REASON.getText());
        bugLog.setNewValue(bugOnlineRepairFailedReasonReq.getRepairFailReason());
        bugLog.setMainId(bugOnlineRepairFailedReasonReq.getId());
        bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
        //往bug日志表中插入一条线上bug内容变更数据
        bugLogMapper.insert(bugLog);

        //如果经办人变了，则添加一条内容变更记录
        if (!operator.equals(bugOnlineDO.getOperator())) {
            BugLogDO bug = new BugLogDO();
            bug.setField(BugFieldEnum.OPERATOR.getText());
            bug.setOldValue(operator);
            bug.setNewValue(bugOnlineDO.getOperator());
            bug.setMainId(bugOnlineRepairFailedReasonReq.getId());
            bug.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bug);
        }

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineRepairFailedMsgEvent(
                        this,
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BaseResult<List<BugOnlineVO>> getByName(BugOnlineGetReq bugOnlineGetReq) {
        List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByName(bugOnlineGetReq.getLinkBugName());
        List<BugOnlineDO> filter = bugOnlineDOList.stream()
                .filter(a -> !Objects.equals(a.getId(), bugOnlineGetReq.getId()) && !Objects.equals(a.getLinkBugId(), bugOnlineGetReq.getId()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filter)) {
            return BaseResult.success(Collections.emptyList());
        }
        List<BugOnlineVO> result = filter.stream().map(BugOnlineCopier.INSTANCE::convertT).collect(Collectors.toList());
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<BugOnlineVO>> getBugOnlineByCustomId(Long customId) {
        if (customId == null) {
            throw new BaseBizRuntimeException("参数有误");
        }
        List<BugOnlineListDO> bugOnlineListDOS = bugOnlineMapper.getByCustomId(customId);
        if (CollectionUtils.isEmpty(bugOnlineListDOS)) {
            return BaseResult.success(new ArrayList<>());
        }
        List<BugOnlineVO> bugOnlineVOList = BugOnlineCopier.INSTANCE.convert(bugOnlineListDOS);
        return BaseResult.success(bugOnlineVOList);
    }

    @Override
    public BaseResult<List<BugOnlineSimpleVO>> getByIds(BugOnlineIdsReq simpleReq) {
        Collection<Long> ids = simpleReq.getIds();
        if (CollUtil.isEmpty(ids)) {
            return BaseResult.success(Collections.emptyList());
        }

        // 查询指定的线上bug
        List<BugOnlineDO> bugOnlineDOs = bugOnlineMapper.getByIds(ids, false);
        if (CollUtil.isEmpty(bugOnlineDOs)) {
            return BaseResult.success(Collections.emptyList());
        }

        // 查询关联的产品线
        List<Long> bugOnlineIds = bugOnlineDOs.stream().map(BaseDO::getId).collect(Collectors.toList());
        List<BugOnlineProductLineDO> linkData = bugOnlineProductLineMapper.getByBugOnlineIdList(bugOnlineIds,
                BizProductLineTypeEnum.BUG_ONLINE.getCode());

        // 关联关系分组处理
        Map<Long, List<BugOnlineProductLineDO>> linkDataGroup = linkData.stream()
                .collect(Collectors.groupingBy(BugOnlineProductLineDO::getBugOnlineId));

        // 获取产品线和业务域的数据
        Set<Long> productLineIds = linkData.stream().map(BugOnlineProductLineDO::getProductLineId).collect(Collectors.toSet());
        Map<Long, PdLineDomainTO> pdLineDomainTOMap = productLineComponent.getMapByIds(productLineIds);

        // 转换，组装数据
        List<BugOnlineSimpleVO> result = bugOnlineDOs.stream().map(BugOnlineCopier.INSTANCE::do2svo).collect(Collectors.toList());
        for (BugOnlineSimpleVO simpleVO : result) {
            Long id = simpleVO.getId();

            // 查询关联关系，获取关联数据
            List<BugOnlineProductLineDO> linkPdLines = linkDataGroup.get(id);
            if (CollUtil.isEmpty(linkPdLines)) {
                continue;
            }
            List<PdLineDomainTO> linkPdLineDomainTOs = linkPdLines.stream()
                    .map(e -> pdLineDomainTOMap.get(e.getProductLineId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // 过滤，填装数据
            List<String> productLineNameList = linkPdLineDomainTOs.stream().map(PdLineDomainTO::getProductLineName).distinct().collect(Collectors.toList());
            List<String> bizDomainNameList = linkPdLineDomainTOs.stream().map(PdLineDomainTO::getBizDomainName).distinct().collect(Collectors.toList());
            simpleVO.setProductLineNameList(productLineNameList);
            simpleVO.setBizDomainNameList(bizDomainNameList);
        }

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Void> attachToBizDemand(BugOnlineAttachToBizReq attachToBizReq) {
        Long id = attachToBizReq.getId();
        List<Long> bizDemandIds = attachToBizReq.getBizDemandIds();
        BugOnlineDO bugOnline = bugOnlineMapper.get(id);
        AssertUtil.notNull(bugOnline, "您选择的线上bug不存在，请刷新后重试");
        AssertUtil.checkState(BugOnlineStatusEnum.canConvertBizDemand(bugOnline.getStatus()),
                "当前状态不允许转化业务需求");
        List<BizDemandDO> bizDemands = bizDemandMapper.getByIds(bizDemandIds);
        AssertUtil.notEmpty(bizDemands, "关联的业务需求不存在，请重新勾选");
        bugOnlineComponent.attachToBizDemands(bugOnline, bizDemandIds, ButtonActionEnum.ATTACH_BUSINESS);
        return BaseResult.success();
    }

    @Override
    public BaseResult<Void> convertBizApply(BugOnlineToBizApplyReq toBizApplyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(toBizApplyReq.getId());
        AssertUtil.notNull(bugOnlineDO, "线上bug不存在");

        BugOnlineConvertBizStatusEnum operateEnum = BugOnlineConvertBizStatusEnum.getByOperate(toBizApplyReq.getOperate());
        AssertUtil.notNull(operateEnum, "操作有误，请检查后重试");

        // 获取技术负责人
        List<BizDomainDO> bizDomain = bugOnlineComponent.getBizDomain(bugOnlineDO.getId());

        Set<PersonAddReq> techOwners = bizDomain.stream()
                .map(e -> new PersonAddReq(e.getTechOwner(), e.getTechOwnerId()))
                .collect(Collectors.toSet());

        if (CollUtil.isEmpty(techOwners)) {
            log.error("[BugOnlineServiceImpl.convertBizApply] 线上bug: {} ,关联的业务域没有技术负责人", toBizApplyReq.getId());
            throw new BaseBizRuntimeException("请确认当前线上bug关联的业务域存在技术负责人");
        }

        // 转业务需求状态
        bugOnlineDO.setConvertBizStatus(operateEnum.getCode());

        if (operateEnum == BugOnlineConvertBizStatusEnum.APPLY) {
            // 转业务需求申请人
            bugOnlineDO.setConvertBizApplyManId(userInfo.getId());
            bugOnlineDO.setConvertBizApplyMan(userInfo.getFullAlias());

            // 更新数据
            bugOnlineMapper.update(bugOnlineDO);

            // 抄送
            personComponent.addIfNotExisted(techOwners, toBizApplyReq.getId(), PersonTypeEnum.BUG_ONLINE_CC.getCode());
            // 通知
            techOwners.stream()
                    .map(PersonAddReq::getUserId)
                    .forEach(techOwnerId -> new BugOnlineToBizApplyEvent(
                            this,
                            bugOnlineDO.getName(),
                            techOwnerId,
                            bugOnlineDO.getId()).send());
        } else {
            Set<String> receivers = new HashSet<>();
            Optional.ofNullable(bugOnlineDO.getOperatorId()).ifPresent(receivers::add);
            Optional.ofNullable(bugOnlineDO.getConvertBizApplyManId()).ifPresent(receivers::add);

            if (CollUtil.isEmpty(receivers)) {
                log.error("[BugOnlineServiceImpl.convertBizApply] 线上bug: {} ,通知接收人为空", toBizApplyReq.getId());
                return BaseResult.success();
            }

            // 通知
            if (operateEnum == BugOnlineConvertBizStatusEnum.AGREE) {
                receivers.forEach(receiver -> new BugOnlineToBizAgreeEvent(
                        this,
                        userInfo.getFullAlias(),
                        bugOnlineDO.getName(),
                        receiver,
                        bugOnlineDO.getId()).send());
            } else if (operateEnum == BugOnlineConvertBizStatusEnum.REJECT) {
                receivers.forEach(receiver -> new BugOnlineToBizRejectEvent(
                        this,
                        userInfo.getFullAlias(),
                        bugOnlineDO.getName(),
                        receiver,
                        bugOnlineDO.getId()).send());
            }
        }

        bugOnlineMapper.updateConvertBizStatus(toBizApplyReq.getId(), operateEnum.getCode());

        return BaseResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Void> startResponse(Long id) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(id);
        AssertUtil.notNull(bugOnlineDO, "线上bug不存在");

        Integer oldStatus = bugOnlineDO.getStatus();
        // 经办人日志
        bugLogComponent.operator(id, bugOnlineDO.getOperator(), userInfo.getFullAlias(), true);

        bugOnlineMapper.updateStatusByIds(CollUtil.newArrayList(id), BugOnlineStatusEnum.START_RESPONSE.getCode());
        bugOnlineDO.setStatus(BugOnlineStatusEnum.START_RESPONSE.getCode());
        bugOnlineDO.setOperatorId(userInfo.getId());
        bugOnlineDO.setOperator(userInfo.getFullAlias());
        bugOnlineMapper.update(bugOnlineDO);
        // 🔔 发布状态变更事件
        eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, BugOnlineStatusEnum.getTextByCode(oldStatus), BugOnlineStatusEnum.START_RESPONSE.getText(), bugOnlineDO.getModifyMan()));

        //往bug日志表中插入一条线上bug状态变更数据
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.START_RESPONSE.getText());
        bugLogDO.setOldValue(BugOnlineStatusEnum.getTextByCode(oldStatus));
        bugLogDO.setNewValue(BugOnlineStatusEnum.START_RESPONSE.getText());
        bugLogDO.setMainId(id);
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        return BaseResult.success();
    }

    @Override
    public BaseResult<Void> acceptance(BugOnlineAcceptanceReq acceptanceReq) {
        if (acceptanceReq.getPass()) {
            //查询线上bug
            BugOnlineDO bugOnlineDO = bugOnlineMapper.get(acceptanceReq.getId());
            AssertUtil.notNull(bugOnlineDO, "线上bug不存在");

            //保存老的状态
            String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

            //线上bug表更新
            bugOnlineMapper.updateStatusByIds(CollUtil.newArrayList(acceptanceReq.getId()), BugOnlineStatusEnum.COMPLETE.getCode());
            // 🔔 发布状态变更事件
            eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.COMPLETE.getText(), bugOnlineDO.getModifyMan()));

            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setAction(ButtonActionEnum.ACCEPTANCE_PASS.getText());
            bugLogDO.setOldValue(oldStatus);
            bugLogDO.setNewValue(BugOnlineStatusEnum.COMPLETE.getText());
            bugLogDO.setMainId(acceptanceReq.getId());
            bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
            //往bug日志表中插入一条线上bug状态变更数据
            bugLogMapper.insert(bugLogDO);

            //bug状态处理人员表插入数据
            bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

            //发送消息
            new BugOnlineAcceptanceMsgEvent(
                    this,
                    bugOnlineDO.getName(),
                    bugOnlineDO.getProposerId(),
                    acceptanceReq.getId()
            ).send();

            List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByLinkBugId(bugOnlineDO.getId());
            bugOnlineDOList.forEach(a -> new BugOnlineResubmitOnlineMsgEvent(
                    this,
                    bugOnlineDO.getName(),
                    a.getProposerId(),
                    bugOnlineDO.getId()).send());
        } else {
            //查询线上bug
            BugOnlineDO bugOnlineDO = bugOnlineMapper.get(acceptanceReq.getId());
            AssertUtil.notNull(bugOnlineDO, "线上bug不存在");

            //保存老的状态
            String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

            //线上bug表更新
            bugOnlineMapper.updateStatusByIds(CollUtil.newArrayList(acceptanceReq.getId()), BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());

            // 🔔 发布状态变更事件
            eventPublisher.publishEvent(new OnlineBugStatusChangeEvent(this, bugOnlineDO, oldStatus, BugOnlineStatusEnum.QUESTION_CONFIRM.getText(), bugOnlineDO.getModifyMan()));

            // 打开次数+1
            bugOnlineMapper.updateIncOpenCount(acceptanceReq.getId());

            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setAction(ButtonActionEnum.ACCEPTANCE_FAILT.getText());
            bugLogDO.setOldValue(oldStatus);
            bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_CONFIRM.getText());
            bugLogDO.setMainId(acceptanceReq.getId());
            bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
            //往bug日志表中插入一条线上bug状态变更数据
            bugLogMapper.insert(bugLogDO);

            //bug状态处理人员表插入数据
            bugLogComponent.insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

            new BugOnlineAcceptanceFailMsgEvent(
                    this,
                    bugOnlineDO.getName(),
                    bugOnlineDO.getOperatorId(),
                    bugOnlineDO.getId()
            ).send();
        }
        return BaseResult.success();
    }

    @Override
    public BaseResult<Integer> getPriority(BugOnlinePriorityGetReq getPriorityReq) {
        return BaseResult.success(bugOnlineComponent.calculatePriority(getPriorityReq));
    }

    /**
     * 判断当前操作人是否为personId或者personId的上级
     */
    Boolean isPermission(String personId) {
        //得到当前操作人账户
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String account = userInfo.getId();

        //如果当前操作人是权限人员，直接返回true
        if (personId.equals(account)) {
            return true;
        }

        //如果当前操作人不是直接权限人，看看是不是直接权限人的上级
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccount(personId);
        accountRequest.setIsLeave(true);
        Set<String> higherLevels = innerUserPersonClient.getAllSuperiorByAccount(accountRequest).getData();

        //判断当前操作人账户是否有权限
        return higherLevels.contains(account);
    }

    /**
     * 判断用户是否为某个职能
     */
    Boolean jobFunctionMatch(String personId, String jobFunction) {
        ArrayList<String> operatorIds = Lists.newArrayList(personId);
        //校验当前经办人职能是否为测试
        List<BaseInfoResponse> personByAccountNew = innerUserPersonClient.getPersonByAccountNew(operatorIds);
        BaseInfoResponse baseInfoResponse = personByAccountNew.get(0);
        if (baseInfoResponse != null) {
            return baseInfoResponse.getJobFunction().contains(jobFunction);
        }
        return true;
    }

    /**
     * 线上bug特殊字段比较
     *
     * @param oldObj 老的线上bug对象
     * @param newObj 新的线上bug对象
     * @return 返回结果集合
     */
    private List<BugLogDO> compareExtField(BugOnlineDO oldObj, BugOnlineDO newObj) {
        final Long bugOnlineId = oldObj.getId();
        List<BugLogDO> bugLogDOList = new ArrayList<>();

        //如果产品线业务这个json字符串变了，要记录一条或多条内容变更日志
        String oldBusiness = oldObj.getBusiness();
        String newBusiness = newObj.getBusiness();
        if (!Objects.equals(oldBusiness, newBusiness)) {
            BusinessMD oldBusinessMD = StrUtil.isEmpty(oldBusiness) ? new BusinessMD() : JSONUtil.toBean(oldBusiness, BusinessMD.class);
            BusinessMD newBusinessMD = StrUtil.isEmpty(newBusiness) ? new BusinessMD() : JSONUtil.toBean(newBusiness, BusinessMD.class);
            oldBusinessMD.setId(bugOnlineId);
            List<BugLogDO> bugLogList = FieldCompareUtil.commonCompare(oldBusinessMD, newBusinessMD, BugLogDO.class);
            bugLogDOList.addAll(bugLogList);
        }

        return bugLogDOList;
    }

    private List<BugLogDO> compareProductLine(List<Long> oldProductLineIdList, List<Long> newProductLineIdList, Long bugOnlineId) {
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        boolean result = CollectionUtils.isEqualCollection(oldProductLineIdList, newProductLineIdList);
        //如果产品线变了记录一条bug内容变更日志
        if (!result) {
            List<Long> mergeProductLineIds = new ArrayList<>();
            mergeProductLineIds.addAll(oldProductLineIdList);
            mergeProductLineIds.addAll(newProductLineIdList);
            Map<Long, String> mergeProductLines = productLineMapper.getByIds(mergeProductLineIds).stream()
                    .collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (v1, v2) -> v2));
            String oldValue = oldProductLineIdList.stream().map(mergeProductLines::get).collect(Collectors.joining(","));
            String newValue = newProductLineIdList.stream().map(mergeProductLines::get).collect(Collectors.joining(","));
            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setField(BugFieldEnum.PRODUCT_LINE.getText());
            bugLogDO.setOldValue(oldValue);
            bugLogDO.setNewValue(newValue);
            bugLogDO.setMainId(bugOnlineId);
            bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogDOList.add(bugLogDO);
        }
        return bugLogDOList;
    }

    private List<BugLogDO> compareModel(List<Long> oldModelIds, List<Long> newModelIds, Long bugOnlineId) {
        oldModelIds = CollectionUtils.isEmpty(oldModelIds) ? new ArrayList<>() : oldModelIds;
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        boolean result = CollectionUtils.isEqualCollection(oldModelIds, newModelIds);
        if (!result) {
            List<Long> mergeModelIds = new ArrayList<>();
            mergeModelIds.addAll(oldModelIds);
            mergeModelIds.addAll(newModelIds);
            Map<Long, String> mergeModels = modelMapper.getByIds(mergeModelIds).stream()
                    .collect(Collectors.toMap(ModelDO::getId, ModelDO::getName, (v1, v2) -> v2));
            String oldValue = oldModelIds.stream().map(mergeModels::get).collect(Collectors.joining(","));
            String newValue = newModelIds.stream().map(mergeModels::get).collect(Collectors.joining(","));
            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setField(BugFieldEnum.MODEL.getText());
            bugLogDO.setOldValue(oldValue);
            bugLogDO.setNewValue(newValue);
            bugLogDO.setMainId(bugOnlineId);
            bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogDOList.add(bugLogDO);
        }
        return bugLogDOList;
    }

    private String updateLinkBug(Long id, Long linkBugId) {
        BugOnlineDO bugOnlineDO = bugOnlineMapper.get(id);
        Long oldLinkBugId = bugOnlineDO.getLinkBugId();
        Long finalBugId = null;
        String tips = StringUtils.EMPTY;
        if (linkBugId != null) {
            List<BugLogDO> bugLogDOList = new ArrayList<>();
            //查找哪些bug关联了当前bug,要将这些bug,重新关联到新的bug上
            List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByLinkBugId(id);
            List<Long> updateIdA = bugOnlineDOList.stream().map(BugOnlineDO::getId).collect(Collectors.toList());
            updateIdA.add(id);

            BugOnlineDO linkBug = bugOnlineMapper.get(linkBugId);
            if (linkBug == null) {
                throw new BaseBizRuntimeException("关联的线上bug不存在");
            }
            finalBugId = linkBug.getId();
            if (linkBug.getLinkBugId() != null) {
                //要关联的bug B可能有关联的bug C   最终取C
                finalBugId = linkBug.getLinkBugId();
                tips = "您关联的bug已存在关联关系，系统直接关联到它关联的bug~";
            }
            if (updateIdA.contains(finalBugId)) {
                throw new BaseBizRuntimeException("关联的bug或其上级bug与当前bug相同,请修改后重试");
            }
            bugOnlineMapper.updateByIds(updateIdA, finalBugId);

            updateIdA.remove(id);
            //处理bug-a: a->关联了当前bugA,现关联finalBugId
            for (Long a : updateIdA) {
                //a删除当前bugA
                bugLogDOList.add(createBugLog(a, a, id, ButtonActionEnum.UN_LINK.getText()));
                bugLogDOList.add(createBugLog(id, a, id, ButtonActionEnum.UN_LINK.getText()));
                //a关联finalBugId
                bugLogDOList.add(createBugLog(a, a, finalBugId, ButtonActionEnum.LINK.getText()));
                bugLogDOList.add(createBugLog(finalBugId, a, finalBugId, ButtonActionEnum.LINK.getText()));
            }
            if (CollectionUtils.isNotEmpty(bugLogDOList)) {
                bugLogMapper.batchInsert(bugLogDOList);
            }
        } else {
            bugOnlineMapper.updateByIds(Lists.newArrayList(id), null);
        }
        log.info("更新关联bug,id:{},oldLinkBugId:{},linkBugId:{},finalBugId:{}", id, oldLinkBugId, linkBugId, finalBugId);
        if (!Objects.equals(oldLinkBugId, finalBugId)) {
            //处理当前bug
            List<BugLogDO> bugLogDOList = new ArrayList<>();
            if (oldLinkBugId != null && finalBugId == null) {
                //由A->B 变成 A->无
                bugLogDOList.add(createBugLog(id, id, oldLinkBugId, ButtonActionEnum.UN_LINK.getText()));
                bugLogDOList.add(createBugLog(oldLinkBugId, id, oldLinkBugId, ButtonActionEnum.UN_LINK.getText()));
            } else if (oldLinkBugId == null) {
                //由A->无 变成 A->B
                bugLogDOList.add(createBugLog(id, id, finalBugId, ButtonActionEnum.LINK.getText()));
                bugLogDOList.add(createBugLog(finalBugId, id, finalBugId, ButtonActionEnum.LINK.getText()));
            } else {
                //由A->B 变成 A->finalBugId
                //删除老的
                bugLogDOList.add(createBugLog(id, id, oldLinkBugId, ButtonActionEnum.UN_LINK.getText()));
                bugLogDOList.add(createBugLog(oldLinkBugId, id, oldLinkBugId, ButtonActionEnum.UN_LINK.getText()));

                bugLogDOList.add(createBugLog(id, id, finalBugId, ButtonActionEnum.LINK.getText()));
                bugLogDOList.add(createBugLog(finalBugId, id, finalBugId, ButtonActionEnum.LINK.getText()));
            }
            bugLogMapper.batchInsert(bugLogDOList);
        }
        return tips;
    }

    private void deleteLinkBug(Long id, Long linkBugId, boolean deleteLinked) {
        //删除关联
        log.info("删除关联bug,id:{},linkBugId:{},deleteLinked:{},", id, linkBugId, deleteLinked);
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        if (linkBugId != null) {
            bugOnlineMapper.updateByIds(Lists.newArrayList(id), null);
            bugLogDOList.add(createBugLog(id, id, linkBugId, ButtonActionEnum.UN_LINK.getText()));
            bugLogDOList.add(createBugLog(linkBugId, id, linkBugId, ButtonActionEnum.UN_LINK.getText()));
        }
        if (deleteLinked) {
            List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByLinkBugId(id);
            bugOnlineDOList.forEach(a ->
                    bugLogDOList.add(createBugLog(a.getId(), a.getId(), id, ButtonActionEnum.UN_LINK.getText())));
            List<Long> updateIds = bugOnlineDOList.stream().map(BugOnlineDO::getId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(updateIds)) {
                bugOnlineMapper.updateByIds(updateIds, null);
            }
        }
        if (CollectionUtils.isNotEmpty(bugLogDOList)) {
            bugLogMapper.batchInsert(bugLogDOList);
        }
    }

    private BugLogDO createBugLog(Long mainId, Long oldValue, Long newValue, String action) {
        return createBugLog(mainId, String.valueOf(oldValue), String.valueOf(newValue), action, BugFieldEnum.LINK_BUG_ONLINE.getText());
    }

    private BugLogDO createBugLog(Long mainId, String oldValue, String newValue, String action, String field) {
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(action);
        bugLogDO.setField(field);
        bugLogDO.setMainId(mainId);
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(newValue);
        return bugLogDO;
    }
}
