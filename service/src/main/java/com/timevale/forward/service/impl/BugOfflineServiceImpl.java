package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BugOfflineService;
import com.timevale.forward.facade.api.query.BugLogQueryList;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.BugOfflineMD;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.handler.AbstractFieldCompareHandler;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.request.AccountRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class BugOfflineServiceImpl extends AbstractFieldCompareHandler<BugOfflineDO> implements BugOfflineService {

    @Resource
    MessageEventPublisher messageEventPublisher;
    @Resource
    private InnerUserPersonClient innerUserPersonClient;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private FileComponent fileComponent;
    @Resource
    private PersonComponent personComponent;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private BugOfflineMapper bugOfflineMapper;
    @Resource
    private BugLogMapper bugLogMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private CommentMapper commentMapper;

    @Override
    public BaseResult<PageQueryResult<BugOfflineVO>> list(BugOfflineQueryList bugOfflineQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BugOfflineListCondition condition = BugOfflineCopier.INSTANCE.convert(bugOfflineQueryList);
        // 时间处理
        condition.setCreateDateLeft(DateUtil.getStartOfDay(condition.getCreateDateLeft()));
        condition.setCreateDateRight(DateUtil.getEndOfDay(condition.getCreateDateRight()));
        condition.setModifyDateLeft(DateUtil.getStartOfDay(condition.getModifyDateLeft()));
        condition.setModifyDateRight(DateUtil.getEndOfDay(condition.getModifyDateRight()));

        // 标志是否有对应数据
        boolean resultIsEmpty = false;
        // 根据tabs添加不同的效果
        String ascription = bugOfflineQueryList.getAscription();
        if (AscriptionEnum.CURRENT_USER.toString().equals(ascription)) {
            condition.setProposerIds(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.RECEIVE.toString().equals(ascription)) {
            condition.setOperatorIds(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.COPIER.toString().equals(ascription)) {
            condition.setCopier(userInfo.getId());
        } else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
            if (AscriptionEnum.TEAM_SUBMIT.toString().equals(ascription)) {
                Set<String> createIdSet = Sets.newHashSet(condition.getProposerIds());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setProposerIds(teamMemberIdList);
            } else if (AscriptionEnum.TEAM_RECEIVE.toString().equals(ascription)) {
                Set<String> operatorSet = Sets.newHashSet(condition.getOperatorIds());
                if (!operatorSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(operatorSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setOperatorIds(teamMemberIdList);
            }
        }
        if (resultIsEmpty) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        // 开始分页
        PageHelper.startPage(bugOfflineQueryList.pageNum, bugOfflineQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        // 查询并转换
        List<BugOfflineListDO> bugOfflineDOList = bugOfflineMapper.selectByCondition(condition);
        List<BugOfflineVO> bugOfflineVOList = bugOfflineDOList.stream().map(BugOfflineCopier.INSTANCE::convert).collect(Collectors.toList());

        // 信息填充
        bugOfflineVOList.forEach(e -> {
            e.setEnvName(BugEnvEnum.getTextByCode(e.getEnv()));
            e.setStatusName(BugStatusEnum.getTextByCode(e.getStatus()));
            e.setSourceName(BugSourceEnum.getTextByCode(e.getSource()));
            e.setBelongName(BugBelongEnum.getTextByCode(e.getBelong()));
            e.setReasonName(BugReasonEnum.getTextByCode(e.getReason()));
            e.setPriorityName(PriorityEnum.getTextChineseByCode(e.getPriority()));
            e.setUnHandleReasonName(BugUnHandleReasonEnum.getTextByCode(e.getUnHandleReason()));
        });

        // 返回分页数据
        PageInfo<BugOfflineListDO> pageInfo = new PageInfo<>(bugOfflineDOList);
        PageQueryResult<BugOfflineVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bugOfflineVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BugOfflineAddReq bugOfflineAddReq) {
        //1.接收表单参数,状态为:bug打开,经办人所选用户,上一阶段经办人为bug提出人,bug数据入库
        BugOfflineDO bugOfflineDO = BugOfflineCopier.INSTANCE.convert(bugOfflineAddReq);
        bugOfflineDO.setOpenCount(0);
        bugOfflineDO.setStatus(BugStatusEnum.OPEN.getCode());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        bugOfflineDO.setLastOperatorId(userInfo.getId());
        bugOfflineDO.setLastOperator(userInfo.getAlias() + "-" + userInfo.getName());
        bugOfflineMapper.insert(bugOfflineDO);

        //2.若存在附件,附件数据入库
        List<FileAddReq> files = bugOfflineAddReq.getFiles();
        fileComponent.add(files, bugOfflineDO.getId(), FileTypeEnum.BUG_OFFLINE.getCode());

        //3.若存在抄送人,抄送人数据入库
        List<PersonAddReq> recipients = bugOfflineAddReq.getRecipients();
        personComponent.add(recipients, bugOfflineDO.getId(), PersonTypeEnum.BUG_OFFLINE_CC.getCode());

        //4.bug日志表记录一条新增数据
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setMainId(bugOfflineDO.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setOldValue(BugStatusEnum.OPEN.getText());
        bugLogDO.setNewValue(BugStatusEnum.OPEN.getText());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        bugLogDO.setAction(ButtonActionEnum.SUBMIT.getText());
        bugLogMapper.insert(bugLogDO);

        //5.消息通知
        messageEventPublisher.publish(new BugOfflineAddMsg(
                this,
                bugOfflineDO.getId(),
                bugOfflineDO.getCreateMan(),
                bugOfflineDO.getOperatorId(),
                bugOfflineDO.getName()
        ));
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(BugOfflineModifyReq bugOfflineModifyReq) {
        BugOfflineDO oldBugOfflineDO = bugOfflineMapper.selectById(bugOfflineModifyReq.getId());
        if (oldBugOfflineDO == null) {
            throw new BaseBizRuntimeException("该线下bug不存在");
        }

        //1.接收表单参数,状态不变,经办人为所选用户,上一阶段经办人不变,bug数据入库
        BugOfflineDO newBugOfflineDO = BugOfflineCopier.INSTANCE.convert(bugOfflineModifyReq);
        bugOfflineMapper.update(newBugOfflineDO);

        //2.更新附件数据
        List<FileAddReq> fileIdList = bugOfflineModifyReq.getFiles();
        fileComponent.update(fileIdList, bugOfflineModifyReq.getId(), FileTypeEnum.BUG_OFFLINE.getCode());

        //3.更新抄送人数据
        List<PersonAddReq> recipientInfoList = bugOfflineModifyReq.getRecipients();
        if (!CollectionUtils.isEmpty(recipientInfoList)) {
            personComponent.update(recipientInfoList, bugOfflineModifyReq.getId(), PersonTypeEnum.BUG_OFFLINE_CC.getCode());
        }

        //4.bug_log记录
        BugOfflineMD oldBugOfflineMD = BugOfflineCopier.INSTANCE.convertToMD(oldBugOfflineDO);
        BugOfflineMD newBugOfflineMD = BugOfflineCopier.INSTANCE.convertToMD(newBugOfflineDO);
        List<BugLogDO> bugLogDOList = commonCompare(oldBugOfflineMD, newBugOfflineMD);
        // 额外判断项目与产品
        bugLogDOList.addAll(compareExtraIfNecessary(oldBugOfflineDO, newBugOfflineDO));
        bugLogDOList.forEach(e -> {
            e.setMainId(bugOfflineModifyReq.getId());
            e.setType(BugLogTypeEnum.OFFLINE.getCode());
        });

        // 特殊判断null和空字符串‘’
        bugLogDOList.removeIf(e -> {
            if(e.getField().equals(BugFieldEnum.DELAY_HANDLE_REASON.getText())){
                String oldValue = e.getOldValue();
                String newValue = e.getNewValue();
                oldValue = StringUtils.isEmpty(oldValue) ? "" : oldValue;
                newValue = StringUtils.isEmpty(newValue) ? "" : newValue;
                return Objects.equals(oldValue, newValue);
            }
            return false;
        });

        if (!CollectionUtils.isEmpty(bugLogDOList)) {
            bugLogMapper.batchInsert(bugLogDOList);
        }

        //5.修改经办人消息通知
        if (!Objects.equals(oldBugOfflineDO.getOperatorId(), newBugOfflineDO.getOperatorId())) {
            messageEventPublisher.publish(new BugOfflineUpdateMsg(
                    this,
                    newBugOfflineDO.getId(),
                    newBugOfflineDO.getOperatorId(),
                    newBugOfflineDO.getName(),
                    BugStatusEnum.getTextByCode(oldBugOfflineDO.getStatus())
            ));
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> transfer(BugOfflineTransferReq bugOfflineTransferReq) {
        log.info("线下bug转交接收参数{}", bugOfflineTransferReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineTransferReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //判断当前操作人是否有权限,如果当前操作人不是经办人或经办人的上级或提出人或提出人的上级，则没有权限
        Boolean operatorResult = isPermission(bugOfflineDO.getOperatorId());
        Boolean proposerResult = isPermission(bugOfflineDO.getProposerId());
        if (!operatorResult && !proposerResult) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //保存老的经办人  花名-真名
        String oldOperator = bugOfflineDO.getOperator();

        //经办人变成转交后的成员
        bugOfflineDO.setOperatorId(bugOfflineTransferReq.getUserId());
        bugOfflineDO.setOperator(bugOfflineTransferReq.getUserName());
        bugOfflineMapper.update(bugOfflineDO);

        //内容变更
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setField(BugFieldEnum.OPERATOR.getText());
        bugLogDO.setOldValue(oldOperator);
        bugLogDO.setNewValue(bugOfflineTransferReq.getUserName());
        bugLogDO.setMainId(bugOfflineTransferReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        //往bug日志表中插入bug内容变更数据
        bugLogMapper.insert(bugLogDO);

        //发送消息
        messageEventPublisher.publish(
                new BugOfflineTransMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOfflineDO.getName(),
                        BugStatusEnum.getTextByCode(bugOfflineDO.getStatus()),
                        bugOfflineDO.getOperatorId(),
                        bugOfflineDO.getId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> unHandle(BugOfflineUnHandleReq bugOfflineUnHandleReq) {
        log.info("不用修复接收参数{}", bugOfflineUnHandleReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineUnHandleReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.OPEN.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<不用修复>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getOperatorId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //获取当前经办人
        String operatorId = bugOfflineDO.getOperatorId();
        String operator = bugOfflineDO.getOperator();

        //保存老的状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //保存老的不用修复原因
        Integer unHandleReason = bugOfflineDO.getUnhandleReason();

        //bug状态变更为"不用修复",上一环节的经办人变成经办人,现在的经办人变成提出人,不用修复原因更新
        bugOfflineDO.setStatus(BugStatusEnum.CONFIRM.getCode());
        bugOfflineDO.setLastOperator(operator);
        bugOfflineDO.setLastOperatorId(operatorId);
        bugOfflineDO.setOperator(bugOfflineDO.getProposer());
        bugOfflineDO.setOperatorId(bugOfflineDO.getProposerId());
        bugOfflineDO.setUnhandleReason(bugOfflineUnHandleReq.getUnHandleReason());
        bugOfflineMapper.update(bugOfflineDO);

        //状态变更
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.NO_REPAIR.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.CONFIRM.getText());
        bugLogDO.setMainId(bugOfflineUnHandleReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //插入bug日志状态变更
        bugLogMapper.insert(bugLogDO);

        //内容变更
        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.UN_HANDLE_REASON.getText());
        //如果有老的不用修复原因，需要给老值赋值
        if (unHandleReason != null) {
            bugLog.setOldValue(BugUnHandleReasonEnum.getTextByCode(unHandleReason));
        }
        bugLog.setNewValue(BugUnHandleReasonEnum.getTextByCode(bugOfflineUnHandleReq.getUnHandleReason()));
        bugLog.setMainId(bugOfflineUnHandleReq.getId());
        bugLog.setType(BugLogTypeEnum.OFFLINE.getCode());
        //插入bug日志内容变更记录
        bugLogMapper.insert(bugLog);

        //发送消息
        messageEventPublisher.publish(
                new BugOfflineNoRepairMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOfflineDO.getName(),
                        bugOfflineDO.getOperatorId(),
                        bugOfflineDO.getId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> agree(BugOfflineReq bugOfflineReq) {
        log.info("线下bug'同意'接收参数{}", bugOfflineReq.getId());
        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<同意>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getOperatorId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //保存bug的当前状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //线下bug的状态变更为关闭
        bugOfflineDO.setStatus(BugStatusEnum.CLOSE.getCode());
        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.AGREE.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.getTextByCode(BugStatusEnum.CLOSE.getCode()));
        bugLogDO.setMainId(bugOfflineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());

        //往bug日志表中插入数据
        bugLogMapper.insert(bugLogDO);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> reject(BugOfflineReq bugOfflineReq) {
        log.info("线下bug'拒绝'接收参数{}", bugOfflineReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<拒绝>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getOperatorId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //获取当前经办人和上一环节经办人
        String operatorId = bugOfflineDO.getOperatorId();
        String operator = bugOfflineDO.getOperator();
        String lastOperatorId = bugOfflineDO.getLastOperatorId();
        String lastOperator = bugOfflineDO.getLastOperator();

        //保存老的状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //保存老的不用修复原因
        Integer unHandleReason = bugOfflineDO.getUnhandleReason();

        //状态变为  bug打开,上一环节的经办人变成当前经办人,当前经办人变成上一环节的经办人，清空不用修复原因
        bugOfflineDO.setStatus(BugStatusEnum.OPEN.getCode());
        bugOfflineDO.setLastOperatorId(operatorId);
        bugOfflineDO.setLastOperator(operator);
        bugOfflineDO.setOperator(lastOperator);
        bugOfflineDO.setOperatorId(lastOperatorId);
        bugOfflineDO.setUnhandleReason(null);
        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REFUSED.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.OPEN.getText());
        bugLogDO.setMainId(bugOfflineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入状态变更数据
        bugLogMapper.insert(bugLogDO);

        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.UN_HANDLE_REASON.getText());
        bugLog.setOldValue(BugUnHandleReasonEnum.getTextByCode(unHandleReason));
        bugLog.setMainId(bugOfflineReq.getId());
        bugLog.setType(BugLogTypeEnum.OFFLINE.getCode());
        //插入bug日志内容变更记录
        bugLogMapper.insert(bugLog);

        //发送消息
        messageEventPublisher.publish(
                new BugOfflineRejectMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOfflineDO.getName(),
                        bugOfflineDO.getOperatorId(),
                        bugOfflineDO.getId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delayHandle(BugOfflineDelayHandleReq bugOfflineDelayHandleReq) {
        log.info("延期修复接收参数{}", bugOfflineDelayHandleReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineDelayHandleReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.OPEN.getCode())
                && !bugOfflineDO.getStatus().equals(BugStatusEnum.REPAIR.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<延期修复>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getOperatorId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //获取当前经办人
        String operatorId = bugOfflineDO.getOperatorId();
        String operator = bugOfflineDO.getOperator();

        //保存老的状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //bug状态变更为"延期修复",上一环节的经办人变成经办人,现在的经办人变成提出人,延期修复原因更新,不用修复原因清空
        bugOfflineDO.setStatus(BugStatusEnum.POSTPONE_REPAIR.getCode());
        bugOfflineDO.setLastOperator(operator);
        bugOfflineDO.setLastOperatorId(operatorId);
        bugOfflineDO.setDelayHandleReason(bugOfflineDelayHandleReq.getDelayHandleReason());
        bugOfflineDO.setUnhandleReason(null);
        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.POSTPONE_REPAIR.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.POSTPONE_REPAIR.getText());
        bugLogDO.setMainId(bugOfflineDelayHandleReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入状态变更记录
        bugLogMapper.insert(bugLogDO);

        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.DELAY_HANDLE_REASON.getText());
        bugLog.setNewValue(bugOfflineDelayHandleReq.getDelayHandleReason());
        bugLog.setMainId(bugOfflineDelayHandleReq.getId());
        bugLog.setType(BugLogTypeEnum.OFFLINE.getCode());
        //插入bug日志内容变更记录
        bugLogMapper.insert(bugLog);

        //发送消息
        messageEventPublisher.publish(
                new BugOfflineDelayRepairMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOfflineDO.getName(),
                        bugOfflineDO.getProposerId(),
                        bugOfflineDO.getId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> doHandle(BugOfflineReq bugOfflineReq) {
        log.info("确认修复接收参数{}", bugOfflineReq.getId());
        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.OPEN.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<确认修复>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getOperatorId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //保存老的状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //线下bug的状态变更为"待修复"，不用修复原因清空
        bugOfflineDO.setStatus(BugStatusEnum.REPAIR.getCode());
        bugOfflineDO.setUnhandleReason(null);
        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.CONFIRM_REPAIR.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.REPAIR.getText());
        bugLogDO.setMainId(bugOfflineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());

        //往bug日志表中插入数据
        bugLogMapper.insert(bugLogDO);

        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> passSelf(BugOfflineReq bugOfflineReq) {
        log.info("自测通过接收参数{}", bugOfflineReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.REPAIR.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<自测通过>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getOperatorId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //获取现在的经办人
        String operatorId = bugOfflineDO.getOperatorId();
        String operator = bugOfflineDO.getOperator();

        //保存老的状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //bug状态变为"待验收",上一环节经办人变成目前经办人，目前经办人变成提出人
        bugOfflineDO.setStatus(BugStatusEnum.ACCEPTANCE.getCode());
        bugOfflineDO.setLastOperator(operator);
        bugOfflineDO.setLastOperatorId(operatorId);

        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.SELF_PASS.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.ACCEPTANCE.getText());
        bugLogDO.setMainId(bugOfflineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());

        //往bug日志表中插入数据
        bugLogMapper.insert(bugLogDO);

        //发送消息
        messageEventPublisher.publish(
                new BugOfflineSelfTestPassMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOfflineDO.getName(),
                        bugOfflineDO.getProposerId(),
                        bugOfflineDO.getId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> accepted(BugOfflineReq bugOfflineReq) {
        log.info("验收通过接收参数{}", bugOfflineReq.getId());
        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.ACCEPTANCE.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<验收通过>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getProposerId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //保存老的状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //线下bug状态变更为"完成"
        bugOfflineDO.setStatus(BugStatusEnum.COMPLETE.getCode());
        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.ACCEPTANCE_PASSED.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.COMPLETE.getText());
        bugLogDO.setMainId(bugOfflineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());

        //往bug日志表中插入数据
        bugLogMapper.insert(bugLogDO);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> acceptFailed(BugOfflineReq bugOfflineReq) {
        log.info("验收失败接收参数{}", bugOfflineReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.ACCEPTANCE.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<验收失败>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getProposerId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //获取当前经办人和上一环节经办人
        String operatorId = bugOfflineDO.getOperatorId();
        String operator = bugOfflineDO.getOperator();
        String lastOperatorId = bugOfflineDO.getLastOperatorId();
        String lastOperator = bugOfflineDO.getLastOperator();

        //保存老的状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //bug状态变为"bug打开",bug的打回次数加一，上一环节的经办人变成当前经办人，当前经办人变成上一环节经办人
        bugOfflineDO.setStatus(BugStatusEnum.OPEN.getCode());
        bugOfflineDO.setReturnCount(bugOfflineDO.getReturnCount() + 1);
        bugOfflineDO.setLastOperatorId(operatorId);
        bugOfflineDO.setLastOperator(operator);
        bugOfflineDO.setOperatorId(lastOperatorId);
        bugOfflineDO.setOperator(lastOperator);

        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.ACCEPTANCE_FAILED.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.OPEN.getText());
        bugLogDO.setMainId(bugOfflineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());

        //往bug日志表中插入数据
        bugLogMapper.insert(bugLogDO);

        messageEventPublisher.publish(
                new BugOfflineCheckFailMsgEvent(
                        this,
                        bugOfflineDO.getName(),
                        bugOfflineDO.getOperatorId(),
                        bugOfflineDO.getId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> reopen(BugOfflineReq bugOfflineReq) {
        log.info("线下bug'重新打开'接收参数{}", bugOfflineReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //得到当前线下bug
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(bugOfflineReq.getId());
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("线下bug不存在。");
        }

        //校验当前状态
        if (!bugOfflineDO.getStatus().equals(BugStatusEnum.COMPLETE.getCode())
                && !bugOfflineDO.getStatus().equals(BugStatusEnum.POSTPONE_REPAIR.getCode())
                && !bugOfflineDO.getStatus().equals(BugStatusEnum.CLOSE.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击<重新打开>按钮");
        }

        //判断当前操作人是否有权限
        Boolean result = isPermission(bugOfflineDO.getProposerId());
        if (!result) {
            throw new BaseBizRuntimeException("您没有操作权限");
        }

        //获取当前经办人和上一环节经办人
        String operatorId = bugOfflineDO.getOperatorId();
        String operator = bugOfflineDO.getOperator();
        String lastOperatorId = bugOfflineDO.getLastOperatorId();
        String lastOperator = bugOfflineDO.getLastOperator();

        //保存老的状态
        String oldValue = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());

        //保存老的延期修复原因
        String delayHandleReason = bugOfflineDO.getDelayHandleReason();

        //状态变为  bug打开,上一环节的经办人变成当前经办人,当前经办人变成上一环节的经办人,清空延期修复原因
        bugOfflineDO.setStatus(BugStatusEnum.OPEN.getCode());
        bugOfflineDO.setLastOperatorId(operatorId);
        bugOfflineDO.setLastOperator(operator);
        bugOfflineDO.setOperatorId(lastOperatorId);
        bugOfflineDO.setOperator(lastOperator);
        //如果老的状态是"完成"需要把bug打回次数加一,重新打开次数加一
        if (oldValue.equals(BugStatusEnum.COMPLETE.getText())) {
            bugOfflineDO.setReturnCount(bugOfflineDO.getReturnCount() + 1);
            bugOfflineDO.setOpenCount(bugOfflineDO.getOpenCount() + 1);
        }
        bugOfflineDO.setDelayHandleReason(null);
        bugOfflineDO.setUnhandleReason(null);
        bugOfflineMapper.update(bugOfflineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.OPEN_AGAIN.getText());
        bugLogDO.setOldValue(oldValue);
        bugLogDO.setNewValue(BugStatusEnum.OPEN.getText());
        bugLogDO.setMainId(bugOfflineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入数据
        bugLogMapper.insert(bugLogDO);

        //如果延期修复原因存在老的值则往bug日志表里插入一条记录
        if (delayHandleReason != null && delayHandleReason != "") {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.DELAY_HANDLE_REASON.getText());
            bugLog.setOldValue(delayHandleReason);
            bugLog.setMainId(bugOfflineReq.getId());
            bugLog.setType(BugLogTypeEnum.OFFLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bugLog);
        }

        messageEventPublisher.publish(
                new BugOfflineOpenAgainMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOfflineDO.getName(),
                        bugOfflineDO.getOperatorId(),
                        bugOfflineDO.getId()
                )
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BugOfflineDetailVO> get(Long id) {
        log.info("查看线下bug详情接收参数:{}", id);
        //校验线下bug是否存在
        BugOfflineDO bugOfflineDO = bugOfflineMapper.selectById(id);
        if (bugOfflineDO == null) {
            throw new BaseBizRuntimeException("您要查询的线下bug不存在。");
        }

        //转化线下bug
        BugOfflineDetailVO bugOfflineDetailVO = BugOfflineCopier.INSTANCE.transform(bugOfflineDO);

        //给bug原因名字赋值
        bugOfflineDetailVO.setReasonName(BugReasonEnum.getTextByCode(bugOfflineDO.getReason()));

        //给线下bug的项目名称赋值
        ProjectDO projectDO = projectMapper.get(bugOfflineDO.getProjectId());
        if (projectDO != null) {
            bugOfflineDetailVO.setProjectName(projectDO.getName());
        }

        //给线下bug的产品线赋值,给线下bug的业务域赋值
        ProductLineDO productLineDO = productLineMapper.selectById(bugOfflineDO.getProductLineId());
        if (productLineDO != null) {
            ProductLineVO productLineVO = ProductLineCopier.INSTANCE.convert(productLineDO);
            bugOfflineDetailVO.setProductLineVO(productLineVO);
            BizDomainDO bizDomainDO = bizDomainMapper.selectById(productLineDO.getBizDomainId());
            //如果业务域不为空，赋值给线下bug的相关属性
            if (bizDomainDO != null) {
                BizDomainVO bizDomainVO = BizDomainCopier.INSTANCE.convert(bizDomainDO);
                bugOfflineDetailVO.setBizDomainVO(bizDomainVO);
            }
        }

        //给线下bug的状态赋值
        String statusName = BugStatusEnum.getTextByCode(bugOfflineDO.getStatus());
        bugOfflineDetailVO.setStatusName(statusName);

        //给线下bug的优先级赋值
        String priorityName = BugPriorityEnum.getTextByCode(bugOfflineDO.getPriority());
        bugOfflineDetailVO.setPriorityName(priorityName);

        //给线下bug的来源赋值
        String sourceName = BugSourceEnum.getTextByCode(bugOfflineDO.getSource());
        bugOfflineDetailVO.setSourceName(sourceName);

        //给线下bug的所属端赋值
        String belongName = BugBelongEnum.getTextByCode(bugOfflineDO.getBelong());
        bugOfflineDetailVO.setBelongName(belongName);

        //给线下爱bug的环境赋值
        String envName = BugEnvEnum.getTextByCode(bugOfflineDO.getEnv());
        bugOfflineDetailVO.setEnvName(envName);

        //给线下bug的浮现频率赋值
        String frequencyName = BugFrequencyEnum.getTextByCode(bugOfflineDO.getFrequency());
        bugOfflineDetailVO.setFrequencyName(frequencyName);

        //给线下bug的不用修复原因赋值
        String reason = BugUnHandleReasonEnum.getTextByCode(bugOfflineDO.getUnhandleReason());
        bugOfflineDetailVO.setUnhandleReasonName(reason);

        //给线下bug的附件集合赋值
        List<FileDO> fileDOList = fileMapper.select(id, FileTypeEnum.BUG_OFFLINE.getCode());
        if (CollectionUtils.isNotEmpty(fileDOList)) {
            List<FileVO> files = fileDOList.stream().map(FileCopier.INSTANCE::change).collect(Collectors.toList());
            bugOfflineDetailVO.setFiles(files);
        }

        //给线下bug的抄送人赋值
        List<PersonDO> personDOList = personMapper.select(PersonListCondition.builder()
                .mainId(id)
                .type(50)
                .build());
        if (CollectionUtils.isNotEmpty(personDOList)) {
            List<PersonVO> personVOList = personDOList.stream().map(PersonCopier.INSTANCE::change).collect(Collectors.toList());
            bugOfflineDetailVO.setRecipientInfoList(personVOList);
        }

        //给bug相关的评论赋值
        List<CommentDO> commentDOList = commentMapper.select(id, CommentTypeEnum.BUG_OFFLINE.getCode());
        if (CollectionUtils.isNotEmpty(commentDOList)) {
            List<CommentVO> commentVOList = commentDOList.stream().map(CommentCopier.INSTANCE::change).collect(Collectors.toList());
            bugOfflineDetailVO.setCommentVOList(commentVOList);
        }

        return BaseResult.success(bugOfflineDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(BugOfflineReq bugOfflineReq) {
        log.info("删除线下bug接收参数:{}", bugOfflineReq.getId());
        //删除线下bug表中的数据
        bugOfflineMapper.deleteById(bugOfflineReq.getId());

        //删除bug日志表中的数据
        bugLogMapper.deleteByBugOfflineId(bugOfflineReq.getId());

        //删除抄送人表person中的数据
        PersonDO personDO = new PersonDO();
        personDO.setMainId(bugOfflineReq.getId());
        personDO.setType(PersonTypeEnum.BUG_OFFLINE_CC.getCode());
        personDO.setIsDeleted(true);
        personMapper.update(personDO);

        //删除评论数据
        commentMapper.deleteByToIdAndType(bugOfflineReq.getId(), CommentTypeEnum.BUG_OFFLINE.getCode());

        //删除附件数据
        FileDO fileDO = new FileDO();
        fileDO.setIsDeleted(true);
        fileDO.setAttacheId(bugOfflineReq.getId());
        fileDO.setType(FileTypeEnum.BUG_OFFLINE.getCode());
        fileMapper.update(fileDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<BugLogVO>> bugLogList(BugLogQueryList bugLogQueryList) {
        PageHelper.startPage(bugLogQueryList.pageNum, bugLogQueryList.pageSize);
        List<BugLogDO> bugLogDOList;
        //如果是状态变更,需要进行筛选出状态变更的数据
        if (bugLogQueryList.getStatusChange()) {
            bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(bugLogQueryList.getId(), bugLogQueryList.getType(), true);
        } else {
            bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(bugLogQueryList.getId(), bugLogQueryList.getType(), false);
        }
        PageInfo<BugLogDO> pageInfo = new PageInfo<>(bugLogDOList);

        PageQueryResult<BugLogVO> pageQueryResult = new PageQueryResult<>();
        //如果没有查询到日志，直接返回空的数据
        if (CollectionUtils.isEmpty(bugLogDOList)) {
            return BaseResult.success(pageQueryResult);
        }

        //获取分页数据
        bugLogDOList = pageInfo.getList();

        //bugLogDO  -->  bugLogVO
        List<BugLogVO> bugLogVOList = bugLogDOList.stream().map(BugLogCopier.INSTANCE::convert).collect(Collectors.toList());
        //给bug内容变更记录类型的名字赋值，给当前时间赋值
        bugLogVOList.forEach(bugLogVO -> {
            bugLogVO.setTypeName(BugLogTypeEnum.getTextByCode(bugLogVO.getType()));
            bugLogVO.setCurrentDate(new Date());
        });

        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        pageQueryResult.setResultList(bugLogVOList);

        return BaseResult.success(pageQueryResult);
    }

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
        Set<String> higherLevels = innerUserPersonClient.getAllSuperiorByAccount(accountRequest).getData();

        //判断当前操作人账户是否有权限
        return higherLevels.contains(account);
    }

    @Override
    protected List<BugLogDO> compareExtraIfNecessary(BugOfflineDO oldBugOfflineDO, BugOfflineDO newBugOfflineDO) {
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        if (!Objects.equals(oldBugOfflineDO.getProjectId(), newBugOfflineDO.getProjectId())) {
            List<ProjectDO> projectDOList = projectMapper
                    .getByIds(Lists.newArrayList(oldBugOfflineDO.getProjectId(), newBugOfflineDO.getProjectId()));
            Map<Long, String> projectMap = projectDOList.stream().collect(Collectors.toMap(BaseDO::getId, ProjectDO::getName));

            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setField(BugFieldEnum.PROJECTS.getText());
            bugLogDO.setOldValue(projectMap.get(oldBugOfflineDO.getProjectId()));
            bugLogDO.setNewValue(projectMap.get(newBugOfflineDO.getProjectId()));
            bugLogDOList.add(bugLogDO);
        }
        if (!Objects.equals(oldBugOfflineDO.getProductLineId(), newBugOfflineDO.getProductLineId())) {
            List<ProductLineDO> productLineDOList = productLineMapper
                    .selectByIds(Lists.newArrayList(oldBugOfflineDO.getProductLineId(), newBugOfflineDO.getProductLineId()));
            Map<Long, String> productLineMap = productLineDOList.stream().collect(Collectors.toMap(BaseDO::getId, ProductLineDO::getName));

            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setField(BugFieldEnum.PRODUCT_LINE.getText());
            bugLogDO.setOldValue(productLineMap.get(oldBugOfflineDO.getProductLineId()));
            bugLogDO.setNewValue(productLineMap.get(newBugOfflineDO.getProductLineId()));
            bugLogDOList.add(bugLogDO);
        }
        return bugLogDOList;
    }
}

























