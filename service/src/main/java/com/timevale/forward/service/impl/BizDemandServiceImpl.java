package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.google.common.base.Objects;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BizDemandInvalidMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandReceivedMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandRejectMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandToReceiveMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
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
    private BugLogMapper bugLogMapper;

    @Resource
    private BugStatusOperatorMapper bugStatusOperatorMapper;

    @Resource
    private BizDemandLogComponent bizDemandLogComponent;

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BizDemandListCondition bizDemandListCondition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);

        // 标志是否有对应数据
        boolean resultIsEmpty = false;
        // 根据tabs添加不同的效果
        String ascription = bizDemandQueryList.getAscription();
        if (ascription.equals(AscriptionEnum.CURRENT_USER.toString())) {
            bizDemandListCondition.setCreateManIdList(Lists.newArrayList(userInfo.getId()));
        } else if (ascription.equals(AscriptionEnum.RECEIVE.toString())) {
            bizDemandListCondition.setReceiveManIdList(Lists.newArrayList(userInfo.getId()));
        } else if (ascription.equals(AscriptionEnum.COPIER.toString())) {
            bizDemandListCondition.setCopier(userInfo.getId());
        } else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
            if (ascription.equals(AscriptionEnum.TEAM_SUBMIT.toString())) {
                Set<String> createIdSet = new HashSet<>(bizDemandListCondition.getCreateManIdList());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                bizDemandListCondition.setCreateManIdList(teamMemberIdList);
            } else if (ascription.equals(AscriptionEnum.TEAM_RECEIVE.toString())) {
                Set<String> receiveIdSet = new HashSet<>(bizDemandListCondition.getReceiveManIdList());
                if (!receiveIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(receiveIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                bizDemandListCondition.setReceiveManIdList(teamMemberIdList);
            }
        }
        if (resultIsEmpty) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        // 开始分页
        PageHelper.startPage(bizDemandQueryList.pageNum, bizDemandQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        return bizDemandComponent.page(bizDemandListCondition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(BizDemandUpdateStatusReq bizDemandUpdateStatusReq) {
        // 修改业务需求状态 —— 作废
        Long bizDemandId = bizDemandUpdateStatusReq.getBizDemandId();
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 记录旧状态
        Integer oldStatus = bizDemandDO.getStatus();

        // 修改业务需求状态
        bizDemandDO.setPlanReleaseDate(CommonConstant.INVALID);
        bizDemandDO.setStatus(BizDemandStatusEnum.INVALID.getCode());
        bizDemandMapper.update(bizDemandDO);

        // 取消产品关联
        productBizDemandMapper.deleteByBizDemandId(bizDemandId);

        // 接收人通知
        messageEventPublisher.publish(new BizDemandInvalidMsgEvent(
                this,
                bizDemandDO.getId(),
                bizDemandDO.getReceiveMan(),
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

        // 日志，产品关联断开
        bizDemandLogComponent.addLogWhenBizDemandInvalid(bizDemandId);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BizDemandAddReq bizDemandAddReq) {
        // 判断主题是否唯一
        if (bizDemandMapper.selectByName(bizDemandAddReq.getName()) != null) {
            throw new BaseBizRuntimeException("该业务需求名称已存在,请修改后重试");
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

        // 判断是否为线上bug转换
        Long bugOnlineId = bizDemandAddReq.getBugOnlineId();
        if (bugOnlineId != null) {
            BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineId);
            if (bugOnlineDO == null) {
                throw new BaseBizRuntimeException("转换需求失败，原线上bug不存在");
            }
        }

        //判断线上bug id是否有值，如果有值的话需要进行和线上bug相关的一些列操作
        if (bizDemandAddReq.getBugOnlineId() != null) {
            special(bizDemandAddReq.getBugOnlineId(), bizDemandDO.getId());
        }

        // 通知需求接收人
        messageEventPublisher.publish(new BizDemandToReceiveMsgEvent(
                this,
                bizDemandDO.getId(),
                bizDemandDO.getCreateMan(),
                bizDemandDO.getReceiveManId(),
                bizDemandDO.getName()
        ));

        // 日志, 状态改为待评估，更改预期上线时间
        bizDemandLogComponent.addLogWhenModifyData(
                StringUtils.EMPTY,
                PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate()),
                bizDemandDO.getId(),
                BizChangeLogFieldEnum.PLAN_RELEASE_DATE.getText(),
                true
        );
        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.EVALUATE.getText(),
                BizDemandStatusEnum.EVALUATE.getText(),
                bizDemandDO.getId(),
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.SUBMIT.getText());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BizDemandDetailVO> getBizDemandById(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 获取对应附件列表
        List<FileDO> fileDOList = fileComponent.select(bizDemandId, FileTypeEnum.BIZ_DEMAND.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);

        // 获取对应抄送人
        List<PersonDO> personDOList = personComponent.select(bizDemandId, PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        List<PersonVO> personVOList = PersonCopier.INSTANCE.transform(personDOList);

        // 获取对应产品线
        ProductLineDO productLineDO = productLineMapper.selectById(bizDemandDO.getProductLineId());
        if (productLineDO == null) {
            throw new BaseBizRuntimeException("业务需求未关联产品线");
        }

        // 信息填充
        BizDemandDetailVO bizDemandDetailVO = BizDemandCopier.INSTANCE.convert(bizDemandDO);

        bizDemandDetailVO.setFileList(fileVOList);
        bizDemandDetailVO.setRecipientInfoList(personVOList);

        bizDemandDetailVO.setProductLineName(productLineDO.getName());
        bizDemandDetailVO.setReasonText(BizDemandReasonEnum.getTextByCode(bizDemandDetailVO.getReason()));
        bizDemandDetailVO.setStatusText(BizDemandStatusEnum.getTextByCode(bizDemandDetailVO.getStatus()));
        bizDemandDetailVO.setPriorityText(PriorityEnum.getTextChineseByCode(bizDemandDetailVO.getPriority()));
        bizDemandDetailVO.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(bizDemandDetailVO.getPlanReleaseDate()));

        // 获取部门链，添加完整部门信息
        Map<Long, GroupResponse> deptMap = bizDemandComponent.getGroupListTreeMap(Lists.newArrayList(bizDemandDO.getDeptId()));
        GroupResponse response = deptMap.get(bizDemandDetailVO.getDeptId());
        if (response == null) {
            log.info("没有找到部门,id为:{}", bizDemandDetailVO.getDeptId());
        } else {
            bizDemandDetailVO.setDeptName(response.getGroupName());
            bizDemandDetailVO.setDeptDeleteFlag(response.getDeleteFlag());
        }

        //获取项目发布时间
        bizDemandDetailVO.setEndDate(bizDemandComponent.getProjectEndDate(bizDemandId));

        // 查看是否为线上bug转换
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectByBizDemandId(bizDemandId);
        if (bugOnlineDO != null) {
            bizDemandDetailVO.setBugOnlineId(bugOnlineDO.getId());
            bizDemandDetailVO.setBugOnlineName(bugOnlineDO.getName());
        }

        return BaseResult.success(bizDemandDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(BizDemandModifyReq bizDemandModifyReq) {
        log.info("业务需求修改接收参数 bizDemandModifyReq = {}", bizDemandModifyReq);

        // 修改业务需求
        BizDemandDO oldBizDemandDO = bizDemandMapper.selectById(bizDemandModifyReq.getId());
        if (oldBizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 判断主题是否唯一
        BizDemandDO checkUniqueName = bizDemandMapper.selectByName(bizDemandModifyReq.getName());
        if (checkUniqueName != null && !checkUniqueName.getId().equals(bizDemandModifyReq.getId())) {
            throw new BaseBizRuntimeException("该业务需求名称已存在,请修改后重试");
        }

        BizDemandDO newBizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandModifyReq);
        bizDemandMapper.update(newBizDemandDO);

        // 添加抄送人数据
        List<PersonAddReq> recipientInfoList = bizDemandModifyReq.getRecipientInfoList();
        if (!CollectionUtils.isEmpty(recipientInfoList)) {
            personComponent.update(recipientInfoList, bizDemandModifyReq.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        }

        // 添加附件
        List<FileAddReq> fileIdList = bizDemandModifyReq.getFileList();
        fileComponent.update(fileIdList, bizDemandModifyReq.getId(), FileTypeEnum.BIZ_DEMAND.getCode());


        if (!Objects.equal(oldBizDemandDO.getReceiveManId(), newBizDemandDO.getReceiveManId())) {
            // 产品线变更带来的接收人变更
            messageEventPublisher.publish(new BizDemandToReceiveMsgEvent(
                    this,
                    oldBizDemandDO.getId(),
                    oldBizDemandDO.getCreateMan(),
                    newBizDemandDO.getReceiveManId(),
                    newBizDemandDO.getName()
            ));
        }

        // 变更日志
        bizDemandLogComponent.addLogWhenModifyData(oldBizDemandDO, newBizDemandDO);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> agree(BizDemandAgreeReq bizDemandAgreeReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 接收，添加预期上线时间
        Long bizDemandId = bizDemandAgreeReq.getBizDemandId();
        Integer planReleaseDate = bizDemandAgreeReq.getPlanReleaseDate();

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 保存旧状态
        Integer oldStatus = bizDemandDO.getStatus();

        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        bizDemandDO.setPlanReleaseDate(planReleaseDate);
        bizDemandMapper.update(bizDemandDO);
        bizDemandMapper.updateReason(bizDemandId, null);

        // 通知需求提交人
        messageEventPublisher.publish(new BizDemandReceivedMsgEvent(
                this,
                bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                bizDemandDO.getCreateManId(),
                bizDemandDO.getName(),
                PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate())
        ));

        bizDemandComponent.updateBizDemandStatusByLinkedProductDemand(bizDemandId);

        // 日志, 状态改为同意
        BizDemandDO newBizDemandDO = bizDemandMapper.selectById(bizDemandId);
        Integer newStatus = newBizDemandDO.getStatus();

        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.getTextByCode(oldStatus),
                BizDemandStatusEnum.getTextByCode(newStatus),
                bizDemandId,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.RECEIVE.getText());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reject(BizDemandRejectReq bizDemandRejectReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 驳回，添加驳回原因
        Long bizDemandId = bizDemandRejectReq.getBizDemandId();
        Integer reason = bizDemandRejectReq.getReason();

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 保存旧状态
        Integer oldStatus = bizDemandDO.getStatus();

        bizDemandDO.setReason(reason);
        bizDemandDO.setPlanReleaseDate(CommonConstant.INVALID);
        bizDemandDO.setStatus(BizDemandStatusEnum.REJECT.getCode());
        bizDemandMapper.update(bizDemandDO);

        // 驳回通知
        messageEventPublisher.publish(new BizDemandRejectMsgEvent(
                this,
                bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                bizDemandDO.getCreateManId(),
                bizDemandDO.getName(),
                BizDemandReasonEnum.getTextByCode(bizDemandDO.getReason())
        ));

        // 日志, 状态改为驳回
        bizDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.getTextByCode(oldStatus),
                BizDemandStatusEnum.REJECT.getText(),
                bizDemandId,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.REJECT.getText());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> transfer(BizDemandTransferReq bizDemandTransferReq) {
        // 转交：修改接收人
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandTransferReq.getId());
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 新旧接收人
        String oldReceiveMan = bizDemandDO.getReceiveMan();
        String newReceiveMan = bizDemandTransferReq.getReceiveMan();
        String newReceiveManId = bizDemandTransferReq.getReceiveManId();

        bizDemandDO.setReceiveMan(newReceiveMan);
        bizDemandDO.setReceiveManId(newReceiveManId);
        bizDemandMapper.update(bizDemandDO);

        // 新旧接受人是否相同
        if(!Objects.equal(oldReceiveMan,newReceiveMan)){
            // 日志记录
            bizDemandLogComponent.addLogWhenModifyData(
                    oldReceiveMan,
                    newReceiveMan,
                    bizDemandDO.getId(),
                    BizChangeLogFieldEnum.RECEIVE_MAN.getText(),
                    true,
                    ButtonActionEnum.TRANSFER.getText());

            // 转交人通知
            messageEventPublisher.publish(new BizDemandToReceiveMsgEvent(
                    this,
                    bizDemandDO.getId(),
                    bizDemandDO.getCreateMan(),
                    bizDemandDO.getReceiveManId(),
                    bizDemandDO.getName()
            ));
        }


        return BaseResult.success(true);
    }

    /**
     * 新增业务需求的时候特殊处理线上bug的方法
     */
    public void special(Long bugOnlineId, Long bizDemandId) {
        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineId);
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“挂起”，“问题上报”，“问题确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.HANG_UP.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.PROBLEM_REPORT.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许转化业务需求");
        }

        //保存老的状态
        String oldStatusName = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        Integer oldStatus = bugOnlineDO.getStatus();

        // 保存旧的bug原因
        String oldReasonName = BugOnlineReasonEnum.getTextByCode(bugOnlineDO.getReason());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.REQUIRED.getCode());
        bugOnlineDO.setReason(BugOnlineReasonEnum.DEMAND_QUESTION.getCode());
        bugOnlineDO.setPrevStatus(oldStatus);
        bugOnlineDO.setBizDemandId(bizDemandId);
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.SHIFT_BUSINESS.getText());
        bugLogDO.setOldValue(oldStatusName);
        bugLogDO.setNewValue(BugOnlineStatusEnum.REQUIRED.getText());
        bugLogDO.setMainId(bugOnlineId);
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        BugLogDO reasonBugLogDO = new BugLogDO();
        reasonBugLogDO.setOldValue(oldReasonName);
        reasonBugLogDO.setNewValue(BugOnlineReasonEnum.DEMAND_QUESTION.getText());
        reasonBugLogDO.setMainId(bugOnlineId);
        reasonBugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        reasonBugLogDO.setField(BugLogFieldEnum.REASON.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(reasonBugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());
    }

    /**
     * 根据线上bug的id，往bug状态人员处理表中插入一条数据
     */
    public void insertToBugStatusOperator(Long bugId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询当前线上bug对应的所有状态变更记录
        List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(bugId
                , BugLogTypeEnum.ONLINE.getCode(), true);

        //按创建时间逆序排列，筛选出最后一条状态变更记录
        List<BugLogDO> collect = bugLogDOList.stream()
                .sorted(Comparator.comparing(BugLogDO::getCreateDate).reversed()).collect(Collectors.toList());
        BugLogDO lastStatusBugLogDO = collect.get(0);

        BugStatusOperatorDO bugStatusOperatorDO = new BugStatusOperatorDO();
        bugStatusOperatorDO.setBugLogId(lastStatusBugLogDO.getId());
        bugStatusOperatorDO.setOperator(userInfo.getAlias() + "-" + userInfo.getName());
        bugStatusOperatorDO.setOperatorId(userInfo.getId());
        //往状态人员处理表里面插入一条数据记录
        bugStatusOperatorMapper.insert(bugStatusOperatorDO);
    }

}
