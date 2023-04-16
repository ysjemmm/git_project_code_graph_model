package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Objects;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.to.PdLineDomainTO;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.BizDemandCustomCopier;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.integration.dock.CrmProjectClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.BaseInfoResponse;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
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

    @Override
    public BaseResult<QueryResultVO<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);

        // 标志是否有对应数据
        boolean resultIsEmpty = false;

        // 根据tabs添加不同的效果
        String ascription = bizDemandQueryList.getAscription();
        if (AscriptionEnum.CURRENT_USER.toString().equals(ascription)) {
            condition.setSubmitManIdList(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.RECEIVE.toString().equals(ascription)) {
            condition.setReceiveManIdList(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.COPIER.toString().equals(ascription)) {
            condition.setCopier(userInfo.getId());
        } else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
            if (AscriptionEnum.TEAM_SUBMIT.toString().equals(ascription)) {
                Set<String> createIdSet = new HashSet<>(condition.getSubmitManIdList());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setSubmitManIdList(teamMemberIdList);
            } else if (AscriptionEnum.TEAM_RECEIVE.toString().equals(ascription)) {
                Set<String> receiveIdSet = new HashSet<>(condition.getReceiveManIdList());
                if (!receiveIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(receiveIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setReceiveManIdList(teamMemberIdList);
            }
        }
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

        return BaseResult.success(bizDemandComponent.page(condition));
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

        // 日期处理
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        condition.setProjectEndDateStart(DateUtil.getStartOfDay(condition.getProjectEndDateStart()));
        condition.setProjectEndDateEnd(DateUtil.getEndOfDay(condition.getProjectEndDateEnd()));

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


        // 关联的项目
        List<Long> linkProjectIds = bizDemandComponent.getLinkProjectIds(bizDemandId);

        // 取消产品关联, 产品关联断开日志
        productBizDemandMapper.deleteByBizDemandId(bizDemandId);
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

        // 刷新客开
        for (Long projectId : linkProjectIds) {
            projectComponent.updateCustomDev(projectId);
        }

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

        // 日志, 状态改为待评估
        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.EVALUATE.getText(),
                BizDemandStatusEnum.EVALUATE.getText(),
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

        bizDemandComponent.updateCustomerPj(bizDemandModifyReq.getId());
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> agree(BizDemandAgreeReq bizDemandAgreeReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 接收，添加预期上线时间
        Long bizDemandId = bizDemandAgreeReq.getBizDemandId();
        Integer planReleaseDate = bizDemandAgreeReq.getPlanReleaseDate();
        Long productLineId = bizDemandAgreeReq.getProductLineId();

        if (productLineId == null) {
            throw new BaseBizRuntimeException("产品线不能为空");
        }

        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 保存旧状态
        Integer oldStatus = bizDemandDO.getStatus();
        Integer oldReason = bizDemandDO.getReason();
        Integer oldPlanReleaseDate = bizDemandDO.getPlanReleaseDate();
        Long oldProductLineId = bizDemandDO.getProductLineId();

        bizDemandDO.setReason(null);
        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        bizDemandDO.setPlanReleaseDate(planReleaseDate);
        bizDemandDO.setProductLineId(productLineId);
        bizDemandMapper.fullUpdate(bizDemandDO);

        // 通知需求提交人
        messageEventPublisher.publish(new BizDemandReceivedMsgEvent(
                this,
                bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
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
    public BaseResult<Boolean> transfer(BizDemandTransferReq bizDemandTransferReq) {
        Boolean result = bizDemandComponent.transfer(bizDemandTransferReq.getId(), bizDemandTransferReq.getReceiveMan(), bizDemandTransferReq.getReceiveManId());
        return BaseResult.success(result);
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

            // 新旧接收人相同则不处理
            if (Objects.equal(oldReceiveMan, newReceiveMan)) {
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
            // 实体
            bizDemandIdList = logDOList.stream().map(BizChangeLogDO::getMainId).collect(Collectors.toList());
            bizDemandMapper.updateReceiveMan(bizDemandIdList, newReceiveMan, newReceiveManId);
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
        if (oldBizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }
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
}
