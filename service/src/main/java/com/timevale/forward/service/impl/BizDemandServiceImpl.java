package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Objects;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.BizDemandUpdateCondition;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.dao.BugOnlineBizDemandMapper;
import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectBizDemandMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandCustomDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectBizDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.client.BizLabelService;
import com.timevale.forward.facade.api.client.LabelCategoryService;
import com.timevale.forward.facade.api.client.ProjectBizDemandService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.BizLabelQueryList;
import com.timevale.forward.facade.api.query.LabelInCategoryQueryList;
import com.timevale.forward.facade.api.request.BatchTransferReq;
import com.timevale.forward.facade.api.request.BizDemandAddReq;
import com.timevale.forward.facade.api.request.BizDemandAgreeReq;
import com.timevale.forward.facade.api.request.BizDemandCompletedAgreeReq;
import com.timevale.forward.facade.api.request.BizDemandCompletedRejectReq;
import com.timevale.forward.facade.api.request.BizDemandCompletedReq;
import com.timevale.forward.facade.api.request.BizDemandCustomAddReq;
import com.timevale.forward.facade.api.request.BizDemandGetReq;
import com.timevale.forward.facade.api.request.BizDemandLinkProjectReq;
import com.timevale.forward.facade.api.request.BizDemandModifyReq;
import com.timevale.forward.facade.api.request.BizDemandNoticeReceiverReq;
import com.timevale.forward.facade.api.request.BizDemandRejectReq;
import com.timevale.forward.facade.api.request.BizDemandResubmitReq;
import com.timevale.forward.facade.api.request.BizDemandSimpleModifyReq;
import com.timevale.forward.facade.api.request.BizDemandTransferReq;
import com.timevale.forward.facade.api.request.BizDemandUpdateStatusReq;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.BizDemandCustomVO;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandSimpleVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.BugOnlineLinkVO;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.LabelCategorySimpleVO;
import com.timevale.forward.facade.api.result.LabelDetailVO;
import com.timevale.forward.facade.api.result.LabelSimpleVO;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductLineAnalyseVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizDemandReasonEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.BugLogFieldEnum;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.enums.BugOnlineReasonEnum;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.BugReasonEnum;
import com.timevale.forward.model.enums.BugStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.PlanReleaseDateEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.to.PdLineDomainTO;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandCustomComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.component.BugLogComponent;
import com.timevale.forward.service.component.BugOnlineComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.LabelComponent;
import com.timevale.forward.service.component.OutBizDealComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProductLineComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.config.CommonConfig;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.BizDemandCustomCopier;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.integration.dock.CrmProjectClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BizDemandApprovedMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandBatchTransferMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandCompletedMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandCompletedRejectMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandInvalidMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandModifyMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandPlanReleaseDateMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandReceivedMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandRejectMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandToReceiveAaginMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandToReceiveMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.duplicate.GroupDuplicateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.BaseInfoResponse;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/14 15:05
 */
@Slf4j
@LogPoint
@RestService
public class BizDemandServiceImpl implements BizDemandService {

    @Resource
    private BizDemandMapper bizDemandMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private ProductBizDemandMapper productBizDemandMapper;
    @Resource
    private InnerUserPersonClient innerUserPersonClient;
    @Resource
    private PersonComponent personComponent;
    @Resource
    private FileComponent fileComponent;
    @Resource
    private MessageEventPublisher messageEventPublisher;
    @Resource
    private BizDemandComponent bizDemandComponent;
    @Resource
    private BugOnlineMapper bugOnlineMapper;
    @Resource
    private BugOnlineBizDemandMapper bugOnlineBizDemandMapper;
    @Resource
    private BugOnlineComponent bugOnlineComponent;
    @Resource
    private BugOfflineMapper bugOfflineMapper;
    @Resource
    private BugLogMapper bugLogMapper;
    @Resource
    private BizDemandLogComponent bizDemandLogComponent;
    @Resource
    private BizChangeLogMapper bizChangeLogMapper;
    @Resource
    private LabelComponent labelComponent;
    @Resource
    private BizDemandCustomComponent bizDemandCustomComponent;
    @Resource
    private OutBizDealComponent outBizDealComponent;
    @Resource
    private BizLabelComponent bizLabelComponent;
    @Resource
    private BugLogComponent bugLogComponent;
    @Resource
    private ProjectComponent projectComponent;
    @Resource
    private ProductLineComponent productLineComponent;
    @Resource
    private CrmProjectClient crmProjectClient;
    @Resource
    private CommonConfig commonConfig;
    @Resource
    private ProjectBizDemandService projectBizDemandService;
    @Resource
    private ProjectBizDemandMapper projectBizDemandMapper;

    @Resource
    private LabelCategoryService labelCategoryService;

    @Resource
    private BizLabelService bizLabelService;

    @Resource
    private GroupDuplicateUtil groupDuplicateUtil;

    @Override
    public BaseResult<QueryResultVO<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList) {
        // 转换查询条件
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);

        String ascription = bizDemandQueryList.getAscription();
        boolean resultIsEmpty = groupDuplicateUtil.isResultIsEmpty(bizDemandQueryList, condition);
        if (resultIsEmpty) {
            return BaseResult.success(ResultUtil.queryResultEmpty());
        }

        // 标签
        if (CollectionUtils.isNotEmpty(bizDemandQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(bizDemandQueryList.getLabelCategoryIds())) {
            List<Long> labelIds = labelComponent.getLabelIds(bizDemandQueryList.getLabelIds(), bizDemandQueryList.getLabelCategoryIds());
            if (CollectionUtils.isEmpty(labelIds) && bizDemandQueryList.getContainLabel()) {
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }
            condition.setLabelIds(labelIds);
        }

        QueryResultVO<BizDemandVO> res = bizDemandComponent.page(condition);
        // 如果tabs为全部，则根据业务域排序
        if (AscriptionEnum.ALL.toString().equals(ascription)) {
            res.getAnalyseVOList().sort(Comparator.comparing(ProductLineAnalyseVO::getBizDomainId));
        }
        if (bizDemandQueryList.getQuerySource() == 1) {
            // 交付项目来源查询需要特殊排序
            List<BizDemandVO> resultList = res.getPageQueryResult().getResultList();
            groupDuplicateUtil.sortByCreateDate(resultList);
        }

        return BaseResult.success(res);
    }

    @Override
    public BaseResult<List<ProductLineAnalyseVO>> listClassify(BizDemandQueryList bizDemandQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);

        // 标志是否有对应数据
        boolean resultIsEmpty = false;
        // 根据tabs添加不同的效果
        String ascription = bizDemandQueryList.getAscription();
        if (ascription.equals(AscriptionEnum.CURRENT_USER.toString())) {
            condition.setSubmitManIdList(Lists.newArrayList(userInfo.getId()));
        } else if (ascription.equals(AscriptionEnum.RECEIVE.toString())) {
            condition.setReceiveManIdList(Lists.newArrayList(userInfo.getId()));
        } else if (ascription.equals(AscriptionEnum.COPIER.toString())) {
            condition.setCopier(userInfo.getId());
        } else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
            if (ascription.equals(AscriptionEnum.TEAM_SUBMIT.toString())) {
                Set<String> createIdSet = new HashSet<>(condition.getSubmitManIdList());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setSubmitManIdList(teamMemberIdList);
            } else if (ascription.equals(AscriptionEnum.TEAM_RECEIVE.toString())) {
                Set<String> receiveIdSet = new HashSet<>(condition.getReceiveManIdList());
                if (!receiveIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(receiveIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setReceiveManIdList(teamMemberIdList);
            }
        }
        if (resultIsEmpty) {
            return BaseResult.success(new ArrayList<>());
        }

        // 包含子部门
        List<Long> deptIdList = condition.getDeptIdList();
        if (CollectionUtils.isNotEmpty(deptIdList)) {
            Map<Long, GroupResponse> groupListTreeMap = bizDemandComponent.getGroupListTreeMap(deptIdList);
            // 替换查询部门id条件
            condition.setDeptIdList(Lists.newArrayList(groupListTreeMap.keySet()));
        }

        // 查询并转换
        List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(condition);
        Map<Long, List<BizDemandListDO>> bizDemandListDOMap = bizDemandListDOList.stream().collect(Collectors.groupingBy(BizDemandListDO::getProductLineId));
        log.info("业务查询产品线分析：{}", bizDemandListDOMap);

        List<ProductLineAnalyseVO> result = new ArrayList<>();
        bizDemandListDOMap.forEach((k, v) -> {
            ProductLineAnalyseVO bizDemandProductLineVO = new ProductLineAnalyseVO();
            Optional<BizDemandListDO> any = v.stream().findAny();
            any.ifPresent(e -> {
                bizDemandProductLineVO.setCount(v.size());
                bizDemandProductLineVO.setProductLineId(e.getProductLineId());
                bizDemandProductLineVO.setProductLineName(e.getProductLineName());
                result.add(bizDemandProductLineVO);
            });
        });

        return BaseResult.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(BizDemandUpdateStatusReq bizDemandUpdateStatusReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 作废
        Long bizDemandId = bizDemandUpdateStatusReq.getBizDemandId();
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 记录旧状态
        Integer oldStatus = bizDemandDO.getStatus();
        Date oldProjectEndDate = bizDemandDO.getProjectEndDate();
        Integer oldPlanReleaseDate = bizDemandDO.getPlanReleaseDate();

        // 修改业务需求状态
        bizDemandDO.setPlanReleaseDate(null);
        bizDemandDO.setProjectEndDate(null);
        bizDemandDO.setStatus(BizDemandStatusEnum.INVALID.getCode());
        bizDemandMapper.fullUpdate(bizDemandDO);

        List<ProjectBizDemandDO> pbdList = projectBizDemandMapper.selectByBizDemandIds(Collections.singletonList(bizDemandId));
        // 取消产品关联, 产品关联断开日志
        productBizDemandMapper.deleteByBizDemandId(bizDemandId);
        if (!pbdList.isEmpty()) {
            projectBizDemandService.linkOrUnlinkBizDemandProject(new BizDemandLinkProjectReq()
                    .setProjectId(pbdList.get(0).getProjectId())
                    .setBizDemandIds(CollUtil.newArrayList(bizDemandDO.getId()))
                    .setType(LinkOrUnLinkEnum.UN_LINK.getCode()));
        }
        bizDemandLogComponent.addLogWhenBizDemandInvalid(bizDemandId);

        // 接收人通知
        messageEventPublisher.publish(new BizDemandInvalidMsgEvent(
                this,
                bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                bizDemandDO.getReceiveManId(),
                bizDemandDO.getName()
        ));

        // 日志, 状态改为作废
        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.getTextByCode(oldStatus),
                BizDemandStatusEnum.INVALID.getText(),
                bizDemandId,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.INVALID.getText());

        if (oldPlanReleaseDate != null) {
            bizDemandLogComponent.addLogWhenModifyData(
                    PlanReleaseDateEnum.getTextByCode(oldPlanReleaseDate),
                    StringUtils.EMPTY,
                    bizDemandId,
                    BizChangeLogFieldEnum.PLAN_RELEASE_DATE.getText(),
                    false);
        }

        if (oldProjectEndDate != null) {
            bizDemandLogComponent.addLogWhenModifyData(
                    DateUtil.parseToString(oldProjectEndDate, DateStyle.YYYY_MM_DD),
                    StringUtils.EMPTY,
                    bizDemandId,
                    BizChangeLogFieldEnum.PROJECT_RELEASE_DATE.getText(),
                    false);
        }

        bizLabelComponent.deleteLabel(bizDemandId, BizTypeEnum.BIZ_DEMAND.getCode());

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BizDemandAddReq bizDemandAddReq) {
        // 判断主题是否唯一
        String bizDemandName = bizDemandAddReq.getName();
        if (bizDemandMapper.selectByName(bizDemandName) != null) {
            throw new BaseBizRuntimeException("该业务需求名称已存在,请修改后重试");
        }
        if (bizDemandName.contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("业务需求名称中请勿包含空格");
        }
        if (bizDemandAddReq.getTargetCustomer().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("目标客户/用户/项目中请勿包含空格");
        }
        if (StringUtils.isNotBlank(bizDemandAddReq.getBizId())) {
            outBizDealComponent.checkBizIdExistence(bizDemandAddReq.getBizId());
        }

        // 新增业务需求
        BizDemandDO bizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandAddReq);
        bizDemandDO.setStatus(BizDemandStatusEnum.EVALUATE.getCode());
        if (bizDemandAddReq.getCustomerDevDemand()) {
            // 客开业务需求新增时如果资源评估人天全部维护，自动变为已接受
            if (ObjectUtils.allNotNull(bizDemandAddReq.getUedTime(), bizDemandAddReq.getFrontTime(),
                    bizDemandAddReq.getQaTime(), bizDemandAddReq.getBackTime(), bizDemandAddReq.getTotalTime()) &&
                    Objects.equal(bizDemandAddReq.getReceiveManId(), bizDemandAddReq.getSubmitManId())) {
                bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
            }
        }
        bizDemandMapper.insert(bizDemandDO);

        List<FileAddReq> fileIdList = bizDemandAddReq.getFileList();
        fileComponent.add(fileIdList, bizDemandDO.getId(), FileTypeEnum.BIZ_DEMAND.getCode());

        // 添加抄送人
        List<PersonAddReq> recipientInfoList = bizDemandAddReq.getRecipientInfoList();
        if (!recipientInfoList.isEmpty()) {
            personComponent.add(recipientInfoList, bizDemandDO.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        }

        // 添加客户信息
        List<BizDemandCustomAddReq> bizDemandCustomAddReqs = bizDemandAddReq.getCustomList();

        if (CollectionUtils.isNotEmpty(bizDemandCustomAddReqs)) {
            bizDemandCustomComponent.add(bizDemandCustomAddReqs, bizDemandDO.getId());
        }

        //判断线上bug id是否有值，如果有值的话需要进行和线上bug相关的一些列操作
        processBugInfo(bizDemandAddReq.getBugOfflineId(), bizDemandAddReq.getBugOnlineId(), bizDemandDO.getId());

        // 通知需求接收人
        messageEventPublisher.publish(new BizDemandToReceiveMsgEvent(
                this,
                bizDemandDO.getId(),
                bizDemandDO.getSubmitMan(),
                bizDemandDO.getReceiveManId(),
                bizDemandDO.getName()
        ));

        // 如果是客开业务需求，还要通知SR
        if (bizDemandAddReq.getCustomerDevDemand()
                && StrUtil.isNotEmpty(bizDemandAddReq.getSrExpertId())
                && ObjectUtil.notEqual(bizDemandAddReq.getReceiveManId(), bizDemandAddReq.getSrExpertId())) {
            new BizDemandToReceiveMsgEvent(
                    this,
                    bizDemandDO.getId(),
                    bizDemandDO.getSubmitMan(),
                    bizDemandDO.getSrExpertId(),
                    bizDemandDO.getName()
            ).send();
        }

        // 日志, 状态改为待评估
        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.getTextByCode(bizDemandDO.getStatus()),
                BizDemandStatusEnum.getTextByCode(bizDemandDO.getStatus()),
                bizDemandDO.getId(),
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.SUBMIT.getText());

        if (StringUtils.isNotBlank(bizDemandAddReq.getBizId())) {
            outBizDealComponent.sendBizDemandRelMsg(bizDemandDO);
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BizDemandDetailVO> getBizDemandById(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("该业务需求不存在");
        }

        // 获取对应附件列表
        List<FileDO> fileDOList = fileComponent.select(bizDemandId, FileTypeEnum.BIZ_DEMAND.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);

        // 获取对应抄送人
        List<PersonDO> personDOList = personComponent.select(bizDemandId, PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        List<PersonVO> personVOList = PersonCopier.INSTANCE.transform(personDOList);

        // 获取对应产品线
        ProductLineDO productLineDO = productLineMapper.selectById(bizDemandDO.getProductLineId());
        AssertUtil.notNull(productLineDO, "业务需求未关联产品线");

        // 信息填充
        BizDemandDetailVO bizDemandDetailVO = BizDemandCopier.INSTANCE.convert(bizDemandDO);
        bizDemandDetailVO.setFileList(fileVOList);
        bizDemandDetailVO.setRecipientInfoList(personVOList);
        bizDemandDetailVO.setEndDate(bizDemandDO.getProjectEndDate());
        bizDemandDetailVO.setProductLineName(productLineDO.getName());
        bizDemandDetailVO.setBizDomainId(productLineDO.getBizDomainId());
        crmProjectClient.getProject(bizDemandDO.getSourceId())
                .ifPresent(p -> bizDemandDetailVO.setCustomerDevProjectName(p.getProjectName()));

        // 获取部门链，添加完整部门信息
        Map<Long, GroupResponse> deptMap = bizDemandComponent.getGroupListTreeMap(Lists.newArrayList(bizDemandDO.getDeptId()));
        GroupResponse response = deptMap.get(bizDemandDetailVO.getDeptId());
        if (response != null) {
            bizDemandDetailVO.setDeptName(response.getGroupName());
            bizDemandDetailVO.setDeptDeleteFlag(response.getDeleteFlag());
        }

        // 查看是否为线上bug转换
        List<Long> bugOnlineIds = bugOnlineBizDemandMapper.getBugOnlineIds(bizDemandId);
        if (!bugOnlineIds.isEmpty()) {
            List<BugOnlineDO> bugOnlineList = bugOnlineMapper.getByIds(bugOnlineIds, false);
            bizDemandDetailVO.setBugOnlineList(bugOnlineList.stream()
                    .map(x -> new BugOnlineLinkVO(x.getId(), x.getName()))
                    .collect(Collectors.toList()));
        }

        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectByBizDemandId(bizDemandId);
        if (bugOfflineDO != null) {
            bizDemandDetailVO.setBugOfflineId(bugOfflineDO.getId());
            bizDemandDetailVO.setBugOfflineName(bugOfflineDO.getName());
        }

        // 获取客户信息
        List<BizDemandCustomDO> bizDemandCustomDOList = bizDemandCustomComponent.selectByBizDemandId(bizDemandId);
        if (CollUtil.isNotEmpty(bizDemandCustomDOList)) {
            List<BizDemandCustomVO> customList = BizDemandCustomCopier.INSTANCE.convertListToVO(bizDemandCustomDOList);
            bizDemandDetailVO.setCustomList(customList);
        }

        return BaseResult.success(bizDemandDetailVO);
    }

    @Override
    public BaseResult<ProductDemandDetailVO> transformBizDemand(Long bizDemandId) {
        ProductDemandDetailVO productDemandDetailVO = new ProductDemandDetailVO();
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
        AssertUtil.notNull(bizDemandDO, "该业务需求不存在");

        // 获取对应抄送人
        List<PersonDO> personDOList = personComponent.select(bizDemandId, PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        List<PersonVO> personVOList = PersonCopier.INSTANCE.transform(personDOList);

        // 获取对应产品线
        ProductLineDO productLineDO = productLineMapper.selectById(bizDemandDO.getProductLineId());
        AssertUtil.notNull(productLineDO, "业务需求未关联产品线");

        // 获取对应附件列表
        List<FileDO> fileDOList = fileComponent.select(bizDemandId, FileTypeEnum.BIZ_DEMAND.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);

        // 产品需求信息填充
        productDemandDetailVO.setName(bizDemandDO.getName());
        productDemandDetailVO.setPriority(bizDemandDO.getPriority());
        productDemandDetailVO.setPriorityName(PriorityEnum.getTextByCode(bizDemandDO.getPriority()));
        //产品线
        productDemandDetailVO.setProductLineVO(ProductLineCopier.INSTANCE.convert(productLineDO));
        // 负责人
        productDemandDetailVO.setOwner(bizDemandDO.getReceiveMan());
        productDemandDetailVO.setOwnerId(bizDemandDO.getReceiveManId());
        // 描述
        productDemandDetailVO.setDesc(bizDemandDO.getDesc());
        // 抄送人
        productDemandDetailVO.setRecipients(personVOList);
        // 附件
        productDemandDetailVO.setFiles(fileVOList);
        // 标签
        LabelInCategoryQueryList labelInCategoryQueryList = new LabelInCategoryQueryList();
        labelInCategoryQueryList.setAuth(true);
        labelInCategoryQueryList.setContainDeleted(false);
        labelInCategoryQueryList.setProductLineIds(Collections.singletonList(productLineDO.getId()));
        labelInCategoryQueryList.setTypes(Collections.singletonList(BizTypeEnum.PRODUCT_DEMAND.getCode()));

        // 获取标签集合
        Set<Long> validLabelIds = Optional.ofNullable(labelCategoryService.getLabelInCategory(labelInCategoryQueryList))
                .filter(BaseResult::ifSuccess)
                .map(BaseResult::getData)
                .orElse(Collections.emptyList())
                .stream()
                .filter(e -> e != null)
                .map(LabelCategorySimpleVO::getLabelSimples)
                .filter(list -> list != null && !list.isEmpty())
                .flatMap(List::stream)
                .map(LabelSimpleVO::getId)
                .filter(e -> e != null)
                .collect(Collectors.toSet()); // 使用Set提高查找效率

        // 获取业务需求已选择的标签
        BizLabelQueryList labelQueryList = new BizLabelQueryList();
        labelQueryList.setBizId(bizDemandId);
        labelQueryList.setType(BizTypeEnum.BIZ_DEMAND.getCode());
        List<LabelDetailVO> labelDetailVOS = Optional.ofNullable(bizLabelService.getSelectedLabel(labelQueryList))
                .filter(BaseResult::ifSuccess)
                .map(BaseResult::getData)
                .orElse(Collections.emptyList());

        // 过滤出有效的标签
        List<LabelDetailVO> labelDetails = labelDetailVOS.stream()
                .filter(label -> validLabelIds.contains(label.getId()))
                .collect(Collectors.toList());

        productDemandDetailVO.setLabelDetailVOS(labelDetails);
        return BaseResult.success(productDemandDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(BizDemandModifyReq bizDemandModifyReq) {
        log.info("业务需求修改接收参数 bizDemandModifyReq = {}", bizDemandModifyReq);

        // 修改业务需求
        BizDemandDO oldBizDemandDO = bizDemandMapper.get(bizDemandModifyReq.getId());
        if (oldBizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }
        if (!Objects.equal(oldBizDemandDO.getPlanReleaseDate(), bizDemandModifyReq.getPlanReleaseDate())
                && !BizDemandStatusEnum.RECEIVED.getCode().equals(oldBizDemandDO.getStatus())
                && !BizDemandStatusEnum.TO_CONFIRM.getCode().equals(oldBizDemandDO.getStatus())
                && !BizDemandStatusEnum.PD_LINKED.getCode().equals(oldBizDemandDO.getStatus())) {
            throw new BaseBizRuntimeException("状态不是已接收,待确认和已关联产品需求时,不能修改预期上线时间");
        }

        // 判断主题是否唯一
        BizDemandDO checkUniqueName = bizDemandMapper.selectByName(bizDemandModifyReq.getName());
        if (checkUniqueName != null && !checkUniqueName.getId().equals(bizDemandModifyReq.getId())) {
            throw new BaseBizRuntimeException("该业务需求名称已存在,请修改后重试");
        }

        BizDemandDO newBizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandModifyReq);
        newBizDemandDO.setStatus(oldBizDemandDO.getStatus());
        newBizDemandDO.setReason(oldBizDemandDO.getReason());
        newBizDemandDO.setProjectEndDate(oldBizDemandDO.getProjectEndDate());
        bizDemandMapper.fullUpdate(newBizDemandDO);

        // 添加抄送人数据
        List<PersonAddReq> recipientInfoList = bizDemandModifyReq.getRecipientInfoList();
        if (!CollectionUtils.isEmpty(recipientInfoList)) {
            personComponent.update(recipientInfoList, bizDemandModifyReq.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        }

        // 客户信息
        bizDemandCustomComponent.update(bizDemandModifyReq.getCustomList(), bizDemandModifyReq.getId());

        // 添加附件
        List<FileAddReq> fileIdList = bizDemandModifyReq.getFileList();
        fileComponent.update(fileIdList, bizDemandModifyReq.getId(), FileTypeEnum.BIZ_DEMAND.getCode());

        // 接收人变更,被驳回 重新提交给接收人
        if (!Objects.equal(oldBizDemandDO.getReceiveManId(), newBizDemandDO.getReceiveManId())) {
            // 判断当前状态≠作废
            if (BizDemandStatusEnum.INVALID.getCode().equals(oldBizDemandDO.getStatus())) {
                throw new BaseBizRuntimeException("已作废业务需求不可修改接收人");
            }

            messageEventPublisher.publish(new BizDemandToReceiveMsgEvent(
                    this,
                    oldBizDemandDO.getId(),
                    oldBizDemandDO.getSubmitMan(),
                    newBizDemandDO.getReceiveManId(),
                    newBizDemandDO.getName()
            ));
        }

        // 判断是否通知接收人
        if (Objects.equal(bizDemandModifyReq.getNotifyReceiveMan(), true)) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            messageEventPublisher.publish(new BizDemandModifyMsgEvent(
                    this,
                    oldBizDemandDO.getId(),
                    userInfo.getAlias() + "-" + userInfo.getName(),
                    newBizDemandDO.getReceiveManId(),
                    newBizDemandDO.getName()
            ));
        }

        // 预期上线时间变更带来的通知
        if (!Objects.equal(oldBizDemandDO.getPlanReleaseDate(), newBizDemandDO.getPlanReleaseDate())) {
            messageEventPublisher.publish(new BizDemandPlanReleaseDateMsgEvent(
                    this,
                    oldBizDemandDO.getId(),
                    oldBizDemandDO.getSubmitManId(),
                    newBizDemandDO.getName(),
                    BizDemandStatusEnum.getTextByCode(oldBizDemandDO.getStatus()),
                    PlanReleaseDateEnum.getTextByCode(newBizDemandDO.getPlanReleaseDate())
            ));
        }

        // 变更日志
        bizDemandLogComponent.addLogWhenModifyData(oldBizDemandDO, newBizDemandDO);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> agree(BizDemandAgreeReq bizDemandAgreeReq) {
        // 修改业务需求状态 —— 接收，添加预期上线时间
        Long bizDemandId = bizDemandAgreeReq.getBizDemandId();
        Integer planReleaseDate = bizDemandAgreeReq.getPlanReleaseDate();
        Long productLineId = bizDemandAgreeReq.getProductLineId();
        String receiveMan = bizDemandAgreeReq.getReceiveMan();
        String receiveManId = bizDemandAgreeReq.getReceiveManId();

        if (productLineId == null) {
            throw new BaseBizRuntimeException("产品线不能为空");
        }

        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
        AssertUtil.notNull(bizDemandDO, "不存在该业务需求");
        if (bizDemandDO.getCustomerDevDemand()) {
            AssertUtil.checkState(ObjectUtils.anyNotNull(bizDemandDO.getUedTime(), bizDemandDO.getFrontTime(),
                            bizDemandDO.getQaTime(), bizDemandDO.getBackTime(), bizDemandDO.getTotalTime()),
                    "请维护好资源评估后再接收需求");
        } else {
            AssertUtil.notNull(bizDemandAgreeReq.getPlanReleaseDate(), "计划上线时间不能为空");
        }

        // 保存旧状态
        Integer oldStatus = bizDemandDO.getStatus();
        Integer oldReason = bizDemandDO.getReason();
        String oldReceiveMan = bizDemandDO.getReceiveMan();
        Integer oldPlanReleaseDate = bizDemandDO.getPlanReleaseDate();
        Long oldProductLineId = bizDemandDO.getProductLineId();

        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        bizDemandDO.setPlanReleaseDate(planReleaseDate);
        bizDemandDO.setProductLineId(productLineId);
        bizDemandDO.setReceiveMan(receiveMan);
        bizDemandDO.setReceiveManId(receiveManId);
        bizDemandMapper.update(bizDemandDO);
        bizDemandMapper.updateReason(bizDemandId, null);

        // 通知需求提交人
        messageEventPublisher.publish(new BizDemandReceivedMsgEvent(
                this,
                bizDemandDO.getId(),
                receiveMan,
                bizDemandDO.getSubmitManId(),
                bizDemandDO.getName(),
                PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate())
        ));

        bizDemandComponent.updateStatus(bizDemandId);

        // 日志, 状态改为同意
        BizDemandDO newBizDemandDO = bizDemandMapper.get(bizDemandId);
        Integer newStatus = newBizDemandDO.getStatus();

        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.getTextByCode(oldStatus),
                BizDemandStatusEnum.getTextByCode(newStatus),
                bizDemandId,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.RECEIVE.getText());

        if (!Objects.equal(oldPlanReleaseDate, planReleaseDate)) {
            bizDemandLogComponent.addLogWhenModifyData(
                    PlanReleaseDateEnum.getTextByCode(oldPlanReleaseDate),
                    PlanReleaseDateEnum.getTextByCode(planReleaseDate),
                    bizDemandDO.getId(),
                    BizChangeLogFieldEnum.PLAN_RELEASE_DATE.getText(),
                    true
            );
        }
        if (!Objects.equal(oldProductLineId, productLineId)) {
            List<ProductLineDO> productLineDOList = productLineMapper.getByIds(Lists.newArrayList(oldProductLineId, productLineId));

            log.info("变更产品线：{}", productLineDOList);
            Optional<ProductLineDO> oldOpt = productLineDOList.stream().filter(e -> oldProductLineId.equals(e.getId())).findAny();
            Optional<ProductLineDO> newOpt = productLineDOList.stream().filter(e -> productLineId.equals(e.getId())).findAny();

            if (oldOpt.isPresent() && newOpt.isPresent()) {
                bizDemandLogComponent.addLogWhenModifyData(
                        oldOpt.get().getName(),
                        newOpt.get().getName(),
                        bizDemandDO.getId(),
                        BizChangeLogFieldEnum.PRODUCT_LINE.getText(),
                        true
                );
            } else {
                log.error("对应产品线不存在: {},{}", oldProductLineId, productLineId);
            }
        }

        String oldReasonText = BizDemandReasonEnum.getTextByCode(oldReason);
        if (StringUtils.isNotEmpty(oldReasonText)) {
            bizDemandLogComponent.addLogWhenModifyData(
                    oldReasonText,
                    StringUtils.EMPTY,
                    bizDemandDO.getId(),
                    BizChangeLogFieldEnum.REASON.getText(),
                    false
            );
        }

        // 如果有申诉标，需要打上成功标
        List<BizLabelDO> labels = bizLabelComponent.get(bizDemandId, BizTypeEnum.BIZ_DEMAND.getCode());
        boolean containBizAppeal = labels.stream().anyMatch(e -> e.getLabelId().equals(commonConfig.getBizDemandAppealLabelId()));
        boolean containDevAppeal = labels.stream().anyMatch(e -> e.getLabelId().equals(commonConfig.getDevDemandAppealLabelId()));
        if (containBizAppeal) {
            Long labelId = commonConfig.getBizDemandApproveLabelId();
            boolean addSuccess = bizLabelComponent.addLabelNx(bizDemandId, labelId, BizTypeEnum.BIZ_DEMAND.getCode());
            if (addSuccess) {
                bizLabelComponent.addLog(bizDemandId, CollUtil.newArrayList(labelId), BizTypeEnum.BIZ_DEMAND.getCode(), true);
            }
        }
        if (containDevAppeal) {
            Long labelId = commonConfig.getDevDemandApproveLabelId();
            boolean addSuccess = bizLabelComponent.addLabelNx(bizDemandId, labelId, BizTypeEnum.BIZ_DEMAND.getCode());
            if (addSuccess) {
                bizLabelComponent.addLog(bizDemandId, CollUtil.newArrayList(labelId), BizTypeEnum.BIZ_DEMAND.getCode(), true);
            }
        }

        // 更新接收人
        bizDemandLogComponent.updateReceiveMan(bizDemandId, oldReceiveMan, bizDemandAgreeReq.getReceiveMan());

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> reject(BizDemandRejectReq bizDemandRejectReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 驳回，添加驳回原因
        Long bizDemandId = bizDemandRejectReq.getBizDemandId();
        Integer reason = bizDemandRejectReq.getReason();

        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 保存旧状态
        Integer oldStatus = bizDemandDO.getStatus();
        Integer oldPlanReleaseDate = bizDemandDO.getPlanReleaseDate();

        bizDemandDO.setReason(reason);
        bizDemandDO.setPlanReleaseDate(null);
        bizDemandDO.setStatus(BizDemandStatusEnum.REJECT.getCode());
        bizDemandMapper.fullUpdate(bizDemandDO);

        // 驳回通知
        messageEventPublisher.publish(new BizDemandRejectMsgEvent(
                this,
                bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                bizDemandDO.getSubmitManId(),
                bizDemandDO.getName(),
                BizDemandReasonEnum.getTextByCode(bizDemandDO.getReason())
        ));

        // 日志
        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.getTextByCode(oldStatus),
                BizDemandStatusEnum.REJECT.getText(),
                bizDemandId,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.REJECT.getText());

        String oldPlanReleaseDateTxt = PlanReleaseDateEnum.getTextByCode(oldPlanReleaseDate);
        if (StringUtils.isNotEmpty(oldPlanReleaseDateTxt)) {
            bizDemandLogComponent.addLogWhenModifyData(
                    oldPlanReleaseDateTxt,
                    StringUtils.EMPTY,
                    bizDemandId,
                    BizChangeLogFieldEnum.PLAN_RELEASE_DATE.getText(),
                    false
            );
        }

        bizDemandLogComponent.addLogWhenModifyData(
                StringUtils.EMPTY,
                BizDemandReasonEnum.getTextByCode(reason),
                bizDemandDO.getId(),
                BizChangeLogFieldEnum.REASON.getText(),
                true
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> transfer(BizDemandTransferReq transferReq) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(transferReq.getId());
        AssertUtil.notNull(bizDemandDO, "该业务需求不存在");

        bizDemandComponent.transfer(transferReq.getId(), transferReq.getReceiveMan(), transferReq.getReceiveManId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 如果为驳回申请，需要添加当前操作人为抄送人
        if (BooleanUtil.isTrue(transferReq.getIsRejectApplication())) {
            PersonAddReq addReq = new PersonAddReq(userInfo.getFullAlias(), userInfo.getId());
            personComponent.addIfNotExisted(CollUtil.newArrayList(addReq), transferReq.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        }

        if (transferReq.getProductLineId() != null
                && !Objects.equal(transferReq.getProductLineId(), bizDemandDO.getProductLineId())) {
            bizDemandMapper.updateProductLineById(transferReq.getId(), transferReq.getProductLineId());
            bizDemandLogComponent.updateProductLine(transferReq.getId(), bizDemandDO.getProductLineId(), transferReq.getProductLineId());
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> bizDemandBatchTransferReceiveMan(BatchTransferReq batchTransferReq) {
        // 参数
        String newReceiveMan = batchTransferReq.getReceiveMan();
        String newReceiveManId = batchTransferReq.getReceiveManId();
        List<Long> bizDemandIdList = batchTransferReq.getIdList();

        // 判空
        if (CollectionUtils.isEmpty(bizDemandIdList)) {
            return BaseResult.success(true);
        }

        // 批处理存储
        List<BizChangeLogDO> logDOList = new ArrayList<>();

        // 获取相关业务需求
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.getByIds(bizDemandIdList);
        for (BizDemandDO e : bizDemandDOList) {
            String oldReceiveMan = e.getReceiveMan();
            String oldReceiveManId = e.getReceiveManId();

            // 新旧接收人相同则不处理
            if (Objects.equal(oldReceiveManId, newReceiveManId)) {
                continue;
            }
            // 日志
            BizChangeLogDO logDO = bizDemandLogComponent.getLogWhenModifyData(
                    oldReceiveMan,
                    newReceiveMan,
                    e.getId(),
                    BizChangeLogFieldEnum.RECEIVE_MAN.getText(),
                    true
            );
            logDOList.add(logDO);
        }

        // 判空
        if (CollectionUtils.isNotEmpty(logDOList)) {
            // 日志
            bizChangeLogMapper.batchInsert(logDOList);
            // 实体更新
            bizDemandIdList = logDOList.stream().map(BizChangeLogDO::getMainId).collect(Collectors.toList());
            bizDemandMapper.updateReceiveMan(bizDemandIdList, newReceiveMan, newReceiveManId);
            // 发送通知
            int count = logDOList.size();
            String initiator = LocalSessionUtils.getUserInfo().getFullAlias();
            String receiveManId = batchTransferReq.getReceiveManId();
            new BizDemandBatchTransferMsgEvent(this, initiator, receiveManId, count).send();
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> bizDemandBatchTransferCreateMan(BatchTransferReq batchTransferReq) {
        // 参数
        String newSubmitMan = batchTransferReq.getReceiveMan();
        String newSubmitManId = batchTransferReq.getReceiveManId();
        List<Long> bizDemandIdList = batchTransferReq.getIdList();

        if (CollectionUtils.isEmpty(bizDemandIdList)) {
            return BaseResult.success(true);
        }

        // 日志处理
        List<BizChangeLogDO> bizChangeLogDOList = new ArrayList<>();
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.getByIds(bizDemandIdList);
        for (BizDemandDO e : bizDemandDOList) {
            String oldSubmitMan = e.getSubmitMan();

            // 新旧相同则不记录日志
            if (Objects.equal(newSubmitMan, oldSubmitMan)) {
                continue;
            }
            BizChangeLogDO logDO = bizDemandLogComponent.getLogWhenModifyData(
                    oldSubmitMan,
                    newSubmitMan,
                    e.getId(),
                    BizChangeLogFieldEnum.CREATE_MAN.getText(),
                    true
            );
            bizChangeLogDOList.add(logDO);
        }

        // 判空
        if (CollectionUtils.isNotEmpty(bizChangeLogDOList)) {
            // 变更提交人及
            Optional<BaseInfoResponse> baseInfo = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(newSubmitManId)).stream().findAny();
            if (!baseInfo.isPresent()) {
                throw new BaseBizRuntimeException("接收人没有默认部门，无法修改");
            }

            // 部门日志
            Long groupId = Long.valueOf(baseInfo.get().getDefaultGroup().getGroupId());
            String newDeptName = bizDemandComponent.getDeptChainName(groupId);

            // 筛选真正需要变更的业务需求id
            bizDemandIdList = bizChangeLogDOList.stream().map(BizChangeLogDO::getMainId).collect(Collectors.toList());
            Set<Long> bizDemandIdSet = new HashSet<>(bizDemandIdList);
            for (BizDemandDO e : bizDemandDOList) {
                if (!bizDemandIdSet.contains(e.getId())) {
                    continue;
                }

                // 部门日志
                String oldDeptName = bizDemandComponent.getDeptChainName(e.getDeptId());
                BizChangeLogDO logDO = bizDemandLogComponent.getLogWhenModifyData(
                        oldDeptName,
                        newDeptName,
                        e.getId(),
                        BizChangeLogFieldEnum.DEPARTMENT.getText(),
                        false
                );
                bizChangeLogDOList.add(logDO);
            }

            bizChangeLogMapper.batchInsert(bizChangeLogDOList);
            bizDemandMapper.updateSubmitMan(bizDemandIdList, newSubmitMan, newSubmitManId, groupId);
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> completed(BizDemandCompletedReq bizDemandCompleted) {
        // 参数
        Long id = bizDemandCompleted.getId();
        String solvePlan = bizDemandCompleted.getSolvePlan() == null ? StringUtils.EMPTY : bizDemandCompleted.getSolvePlan();
        Long productLineId = bizDemandCompleted.getProductLineId();

        if (productLineId == null) {
            throw new BaseBizRuntimeException("产品线不能为空");
        }

        BizDemandDO bizDemandDO = bizDemandMapper.get(id);
        Integer oldStatus = bizDemandDO.getStatus();
        Integer newStatus = BizDemandStatusEnum.TO_CONFIRM.getCode();

        // 旧数据
        String oldRejectReason = bizDemandDO.getRejectReason();
        String oldSolvePlan = bizDemandDO.getSolvePlan();
        Long oldProductLineId = bizDemandDO.getProductLineId();

        // 更新
        bizDemandDO.setRejectReason(StringUtils.EMPTY);
        bizDemandDO.setStatus(newStatus);
        bizDemandDO.setSolvePlan(solvePlan);
        bizDemandDO.setProductLineId(productLineId);
        bizDemandMapper.fullUpdate(bizDemandDO);

        // 日志
        String oldValue = BizDemandStatusEnum.getTextByCode(oldStatus);
        String newValue = BizDemandStatusEnum.getTextByCode(newStatus);
        bizDemandLogComponent.addLogWhenModifyData(
                oldValue,
                newValue,
                id,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.COMPLETED_NOT_DEV.getText());

        if (!Objects.equal(oldSolvePlan, bizDemandCompleted.getSolvePlan())) {
            bizDemandLogComponent.addLogWhenModifyData(
                    oldSolvePlan,
                    bizDemandCompleted.getSolvePlan(),
                    id,
                    BizChangeLogFieldEnum.SOLVE_PLAN.getText(),
                    true);
        }

        if (!Objects.equal(oldProductLineId, productLineId)) {
            List<ProductLineDO> productLineDOList = productLineMapper.getByIds(Lists.newArrayList(oldProductLineId, productLineId));

            log.info("变更产品线：{}", productLineDOList);
            Optional<ProductLineDO> oldOpt = productLineDOList.stream().filter(e -> oldProductLineId.equals(e.getId())).findAny();
            Optional<ProductLineDO> newOpt = productLineDOList.stream().filter(e -> productLineId.equals(e.getId())).findAny();

            if (oldOpt.isPresent() && newOpt.isPresent()) {
                bizDemandLogComponent.addLogWhenModifyData(
                        oldOpt.get().getName(),
                        newOpt.get().getName(),
                        bizDemandDO.getId(),
                        BizChangeLogFieldEnum.PRODUCT_LINE.getText(),
                        true
                );
            } else {
                log.error("对应产品线不存在: {},{}", oldProductLineId, productLineId);
            }
        }

        if (StringUtils.isNotEmpty(oldRejectReason)) {
            bizDemandLogComponent.addLogWhenModifyData(
                    oldRejectReason,
                    StringUtils.EMPTY,
                    id,
                    BizChangeLogFieldEnum.REJECT_REASON.getText(),
                    true);
        }
        // 通知需求提交人
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        messageEventPublisher.publish(new BizDemandCompletedMsgEvent(
                this,
                bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                bizDemandDO.getSubmitManId(),
                bizDemandDO.getName()
        ));

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> completedAgree(BizDemandCompletedAgreeReq bizDemandCompletedAgreeReq) {
        Long id = bizDemandCompletedAgreeReq.getId();

        BizDemandDO oldBizDemandDO = bizDemandMapper.get(id);
        Integer oldStatus = oldBizDemandDO.getStatus();
        Integer newStatus = BizDemandStatusEnum.COMPLETED.getCode();

        // 更新
        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setId(id);
        bizDemandDO.setStatus(newStatus);
        bizDemandMapper.update(bizDemandDO);

        // 日志
        String oldStatusText = BizDemandStatusEnum.getTextByCode(oldStatus);
        String newStatusText = BizDemandStatusEnum.getTextByCode(newStatus);
        bizDemandLogComponent.addLogWhenModifyData(
                oldStatusText,
                newStatusText,
                id,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.AGREE.getText());

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> completedReject(BizDemandCompletedRejectReq bizDemandCompletedRejectReq) {
        Long id = bizDemandCompletedRejectReq.getId();
        String reason = bizDemandCompletedRejectReq.getRejectReason();

        BizDemandDO oldBizDemandDO = bizDemandMapper.get(id);
        Integer oldStatus = oldBizDemandDO.getStatus();
        Integer newStatus = BizDemandStatusEnum.RECEIVED.getCode();

        // 更新
        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setId(id);
        bizDemandDO.setStatus(newStatus);
        bizDemandDO.setRejectReason(reason);
        bizDemandMapper.update(bizDemandDO);

        // 日志
        String oldStatusText = BizDemandStatusEnum.getTextByCode(oldStatus);
        String newStatusText = BizDemandStatusEnum.getTextByCode(newStatus);
        bizDemandLogComponent.addLogWhenModifyData(
                oldStatusText,
                newStatusText,
                id,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.REFUSED.getText());
        bizDemandLogComponent.addLogWhenModifyData(
                StringUtils.EMPTY,
                reason,
                id,
                BizChangeLogFieldEnum.REJECT_REASON.getText(),
                true
        );


        // 通知
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        messageEventPublisher.publish(new BizDemandCompletedRejectMsgEvent(
                this,
                oldBizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                oldBizDemandDO.getReceiveManId(),
                oldBizDemandDO.getName()
        ));

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> reSubmit(BizDemandResubmitReq bizDemandResubmitReq) {
        Long bizDemandId = bizDemandResubmitReq.getId();
        String name = bizDemandResubmitReq.getName();
        BizDemandDO oldBizDemandDO = bizDemandMapper.get(bizDemandId);
        AssertUtil.notNull(oldBizDemandDO,"不存在该业务需求");
        if (!bizDemandResubmitReq.getIsAppeal()) {
            AssertUtil.notBlank(bizDemandResubmitReq.getReceiveManId(), "业务需求接收人不能为空");
            AssertUtil.notBlank(bizDemandResubmitReq.getReceiveMan(), "业务需求接收人不能为空");
        } else {
            if (oldBizDemandDO.getCustomerDevDemand()) {
                bizDemandResubmitReq.setReceiveManId(commonConfig.getDevDemandAcceptUserId());
                bizDemandResubmitReq.setReceiveMan(commonConfig.getDevDemandAcceptUserName());
            } else {
                bizDemandResubmitReq.setReceiveManId(commonConfig.getBizDemandAcceptUserId());
                bizDemandResubmitReq.setReceiveMan(commonConfig.getBizDemandAcceptUserName());
            }
        }
        Integer oldStatus = oldBizDemandDO.getStatus();
        BizDemandDO checkUniqueName = bizDemandMapper.selectByName(name);
        if (checkUniqueName != null && !checkUniqueName.getId().equals(bizDemandId)) {
            throw new BaseBizRuntimeException("该业务需求名称已存在,请修改后重试");
        }
        // 日志, 状态改为待评估
        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.REJECT.getText(),
                BizDemandStatusEnum.EVALUATE.getText(),
                bizDemandId,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.RESUBMIT.getText());

        String oldReasonText = BizDemandReasonEnum.getTextByCode(oldBizDemandDO.getReason());
        if (StringUtils.isNotEmpty(oldReasonText)) {
            bizDemandLogComponent.addLogWhenModifyData(
                    oldReasonText,
                    StringUtils.EMPTY,
                    bizDemandId,
                    BizChangeLogFieldEnum.REASON.getText(),
                    false
            );
        }
        oldBizDemandDO.setStatus(BizDemandStatusEnum.EVALUATE.getCode());
        oldBizDemandDO.setReason(null);
        oldBizDemandDO.setReceiveMan(bizDemandResubmitReq.getReceiveMan());
        oldBizDemandDO.setReceiveManId(bizDemandResubmitReq.getReceiveManId());
        bizDemandMapper.fullUpdate(oldBizDemandDO);

        if (oldBizDemandDO.getCustomerDevDemand() && bizDemandResubmitReq.getIsAppeal()) {
            // 客开需求需要添加标签：客开需求提交申诉
            Long labelId = commonConfig.getDevDemandAppealLabelId();
            Map<Long, List<BizLabelSimpleVO>> bizLabelMap =
                    bizLabelComponent.getBizLabelMap(Collections.singletonList(oldBizDemandDO.getId()),
                            BizTypeEnum.BIZ_DEMAND.getCode());
            if (bizLabelMap.isEmpty()) {
                addBizLabel(oldBizDemandDO, labelId);
            } else {
                List<BizLabelSimpleVO> labels = bizLabelMap.get(bizDemandId);
                if (labels.stream().map(BizLabelSimpleVO::getId)
                        .noneMatch(x -> Objects.equal(x, commonConfig.getDevDemandAppealLabelId()))) {
                    addBizLabel(oldBizDemandDO, labelId);
                }
            }
        } else if (BizDemandStatusEnum.REJECT.getCode().equals(oldStatus) && bizDemandResubmitReq.getIsAppeal()) {
            Long labelId = commonConfig.getBizDemandAppealLabelId();
            boolean addSuccess = bizLabelComponent.addLabelNx(bizDemandId, labelId, BizTypeEnum.BIZ_DEMAND.getCode());
            if (addSuccess) {
                bizLabelComponent.addLog(bizDemandId, CollUtil.newArrayList(labelId), BizTypeEnum.BIZ_DEMAND.getCode(), true);
            }
        }
        messageEventPublisher.publish(new BizDemandToReceiveAaginMsgEvent(
                this,
                oldBizDemandDO.getId(),
                oldBizDemandDO.getSubmitMan(),
                oldBizDemandDO.getReceiveManId(),
                name
        ));
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<BizDemandSimpleVO>> getSimpleBizDemands(BizDemandGetReq bizDemandGetReq) {
        List<Long> ids = bizDemandGetReq.getIds();
        List<Integer> status = bizDemandGetReq.getStatus();
        String sourceId = bizDemandGetReq.getSourceId();

        // 参数校验
        if (CollUtil.isEmpty(ids) && CollUtil.isEmpty(status) && StrUtil.isEmpty(sourceId)) {
            return BaseResult.success(Lists.emptyList());
        }

        // 查询业务需求
        List<BizDemandDO> bizDemandDOs = bizDemandMapper.getSimpleBizDemands(ids, status, sourceId);
        if (CollUtil.isEmpty(bizDemandDOs)) {
            return BaseResult.success(Lists.emptyList());
        }

        // 查询关联的产品线业务域信息
        Set<Long> productLineIds = bizDemandDOs.stream().map(BizDemandDO::getProductLineId).collect(Collectors.toSet());
        Map<Long, PdLineDomainTO> pdLineDomainTOMap = productLineComponent.getMapByIds(productLineIds);

        // 转换返回信息
        List<BizDemandSimpleVO> result = bizDemandDOs.stream()
                .map(e -> BizDemandCopier.INSTANCE.do2svo(e, pdLineDomainTOMap.get(e.getProductLineId())))
                .collect(Collectors.toList());

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<BizDemandVO>> getBizDemandByCustomId(Long customId) {
        if (customId == null) {
            throw new BaseBizRuntimeException("参数有误");
        }
        List<BizDemandListDO> bizDemandDOS = bizDemandMapper.selectByCustomId(customId);
        if (CollectionUtils.isEmpty(bizDemandDOS)) {
            return BaseResult.success(new ArrayList<>());
        }
        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandDOS);
        // 信息填充
        for (BizDemandVO bizDemandVO : bizDemandVOList) {

            bizDemandVO.setStatusText(BizDemandStatusEnum.getTextByCode(bizDemandVO.getStatus()));
            bizDemandVO.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(bizDemandVO.getPlanReleaseDate()));
        }
        return BaseResult.success(bizDemandVOList);
    }

    @Override
    public BaseResult<Boolean> noticeReceiver(BizDemandNoticeReceiverReq receiverReq) {
        log.info("开发资源申请流程通过,通知需求接收人:{}", receiverReq.getBizDemandIds());
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.getByIds(receiverReq.getBizDemandIds());
        bizDemandDOList.forEach(a ->
                messageEventPublisher.publish(new BizDemandApprovedMsgEvent(
                        this,
                        a.getId(),
                        a.getReceiveManId(),
                        a.getName())
                ));
        return BaseResult.success(true);
    }

    @Override
    public ProjectVO findLinkProject(Long bizDemandId) {
        log.info("业务需求-项目清单接收参数:{}", bizDemandId);
        ProjectDO projectDO = projectComponent.getByBizDemandId(bizDemandId);
        return ProjectCopier.INSTANCE.transform(projectDO);
    }

    @Override
    public BaseResult<Void> simpleModifyBizDemands(BizDemandSimpleModifyReq req) {
        if (CollectionUtils.isEmpty(req.getBizDemandIds()) ||
            req.getCustomerDevDemand() == null) {
            return BaseResult.success();
        }
        bizDemandMapper.updateConditional(new BizDemandUpdateCondition()
                .setIds(req.getBizDemandIds())
                .setCustomerDevDemand(req.getCustomerDevDemand()));
        return BaseResult.success();
    }

    /**
     * 新增业务需求的时候特殊处理线上bug的方法
     */
    private void processBugInfo(Long bugOfflineId, Long bugOnlineId, Long bizDemandId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        // 判断是否为线下bug转换
        if (bugOfflineId != null) {
            BugOfflineDO bugOfflineDO = bugOfflineMapper.get(bugOfflineId);
            if (bugOfflineDO == null) {
                throw new BaseBizRuntimeException("转换需求失败，原线下bug不存在");
            }
            if (!BugStatusEnum.canConvertBizDemand(bugOfflineDO.getStatus())) {
                throw new BaseBizRuntimeException("当前状态不允许转化业务需求");
            }
            //保存老的状态
            Integer oldStatus = bugOfflineDO.getStatus();
            String oldStatusName = BugStatusEnum.getTextByCode(oldStatus);
            // 保存旧的bug原因
            String oldReasonName = BugOnlineReasonEnum.getTextByCode(bugOfflineDO.getReason());

            bugOfflineDO.setStatus(BugStatusEnum.REQUIRED.getCode());
            bugOfflineDO.setReason(BugReasonEnum.DEMAND.getCode());
            bugOfflineDO.setBizDemandId(bizDemandId);
            bugOfflineDO.setPrevStatus(oldStatus);
            //线上bug表更新
            bugOfflineMapper.update(bugOfflineDO);

            BugLogDO bugLogDO = createBugLog(bugOfflineId
                    , oldStatusName
                    , BugStatusEnum.REQUIRED.getText()
                    , ButtonActionEnum.SHIFT_BUSINESS.getText()
                    , BugLogFieldEnum.STATUS.getText()
                    , BugLogTypeEnum.OFFLINE.getCode());
            bugLogMapper.insert(bugLogDO);

            BugLogDO reasonBugLogDO = createBugLog(bugOfflineId
                    , oldReasonName
                    , BugReasonEnum.DEMAND.getText()
                    , null
                    , BugLogFieldEnum.REASON.getText()
                    , BugLogTypeEnum.OFFLINE.getCode());

            bugLogMapper.insert(reasonBugLogDO);

            //bug状态处理人员表插入数据
            bugLogComponent.insertToBugStatusOperator(bugOfflineId, userInfo.getId(), userInfo.getFullAlias(), BugLogTypeEnum.OFFLINE.getCode());
        }
        if (bugOnlineId != null) {
            BugOnlineDO bugOnlineDO = bugOnlineMapper.get(bugOnlineId);
            AssertUtil.notNull(bugOnlineDO, "转换需求失败，原线上bug不存在");
            AssertUtil.checkState(BugOnlineStatusEnum.canConvertBizDemand(bugOnlineDO.getStatus()), "当前状态不允许转化业务需求");

            // 转换需求
            bugOnlineComponent.attachToBizDemands(bugOnlineDO, Collections.singletonList(bizDemandId),
                    ButtonActionEnum.SHIFT_BUSINESS);
        }
    }

    private BugLogDO createBugLog(Long mainId, String oldValue, String newValue, String action, String field, Integer bugType) {
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(action);
        bugLogDO.setField(field);
        bugLogDO.setMainId(mainId);
        bugLogDO.setType(bugType);
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(newValue);
        return bugLogDO;
    }

    private void addBizLabel(BizDemandDO bizDemand, Long labelId) {
        List<Long> labelIds = Collections.singletonList(labelId);
        bizLabelComponent.addLabel(bizDemand.getId(), labelIds,
                BizTypeEnum.BIZ_DEMAND.getCode());
        bizLabelComponent.addLog(bizDemand.getId(), labelIds,
                BizTypeEnum.BIZ_DEMAND.getCode(), true);
    }
}
