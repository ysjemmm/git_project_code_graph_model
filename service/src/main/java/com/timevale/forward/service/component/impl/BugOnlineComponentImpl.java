package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.BugOnlineBizDemandMapper;
import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.BugOnlineStatusOperatorDO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BugLogComponent;
import com.timevale.forward.service.component.BugOnlineComponent;
import com.timevale.forward.service.component.BugOnlineStatusOperatorComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.observer.event.BugOnlineConfirmMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class BugOnlineComponentImpl implements BugOnlineComponent {

    @Resource
    private BugOnlineMapper bugOnlineMapper;

    @Resource
    private BugLogMapper bugLogMapper;

    @Resource
    private BugLogComponent bugLogComponent;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private BugOnlineStatusOperatorComponent bugOnlineStatusOperatorComponent;

    @Resource
    private BugOnlineBizDemandMapper bugOnlineBizDemandMapper;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Override
    public void autoCloseBugIfBeConfirm(int autoCloseLimitDay) {
        log.info("待确认线上bug自动关闭-开始:{}", autoCloseLimitDay);
        List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByStatus(Lists.newArrayList(BugOnlineStatusEnum.BE_CONFIRM.getCode()));
        Date today = new Date();
        List<Long> updateBugIds = new ArrayList<>();
        List<Long> bugOnlineIds = bugOnlineDOList.stream().map(BugOnlineDO::getId).collect(Collectors.toList());
        bugOnlineIds.forEach(a -> {
            List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(a, BugLogTypeEnum.ONLINE.getCode(), true)
                    .stream().filter(b -> BugOnlineStatusEnum.BE_CONFIRM.getText().equals(b.getNewValue())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bugLogDOList)) {
                bugLogDOList.sort(Comparator.comparing(BugLogDO::getCreateDate).reversed());
                Date createDate = bugLogDOList.get(0).getCreateDate();
                if (DateUtil.getIntervalDays(today, createDate) >= autoCloseLimitDay) {
                    updateBugIds.add(a);
                }
            }
        });
        if (CollectionUtils.isNotEmpty(updateBugIds)) {
            bugOnlineMapper.updateStatusByIds(updateBugIds, BugOnlineStatusEnum.CLOSE.getCode());
            updateBugIds.forEach(this::addLog);
        }
        log.info("待确认线上bug自动关闭-完成,更新id:{}", updateBugIds);
    }

    @Override
    public void autoNoticeCloseBugIfBeConfirm(int autoCloseLimitDay) {
        log.info("待确认线上bug自动关闭前钉钉通知-开始:{}", autoCloseLimitDay);
        List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByStatus(Lists.newArrayList(BugOnlineStatusEnum.BE_CONFIRM.getCode()));
        Date today = new Date();
        List<BugOnlineDO> noticeBugOnlies = new ArrayList<>();
        List<Long> noticeIds = new ArrayList<>();
        bugOnlineDOList.forEach(a -> {
            List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(a.getId(), BugLogTypeEnum.ONLINE.getCode(), true)
                    .stream().filter(b -> BugOnlineStatusEnum.BE_CONFIRM.getText().equals(b.getNewValue())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bugLogDOList)) {
                bugLogDOList.sort(Comparator.comparing(BugLogDO::getCreateDate).reversed());
                Date createDate = bugLogDOList.get(0).getCreateDate();
                if (DateUtil.getIntervalDays(today, createDate) >= autoCloseLimitDay) {
                    noticeBugOnlies.add(a);
                    noticeIds.add(a.getId());
                }
            }
        });
        if (CollectionUtils.isNotEmpty(noticeBugOnlies)) {
            // 发送通知
            noticeBugOnlies.forEach(n -> messageEventPublisher.publish(new BugOnlineConfirmMsgEvent(
                    this,
                    n.getName(),
                    n.getProposerId(),
                    n.getId()
            )));
        }
        log.info("待确认线上bug自动关闭前钉钉通知-结束:{}", noticeIds);

    }

    @Override
    public void attachToBizDemands(BugOnlineDO bugOnlineDO, Collection<Long> bizDemandIds, boolean logToBizDemands) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        Long bugOnlineId = bugOnlineDO.getId();
        String oldStatusName = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        Integer oldStatus = bugOnlineDO.getStatus();

        // 保存旧的bug原因
        String oldReasonName = BugOnlineReasonEnum.getTextByCode(bugOnlineDO.getReason());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.REQUIRED.getCode());
        bugOnlineDO.setReason(BugOnlineReasonEnum.DEMAND_QUESTION.getCode());
        bugOnlineDO.setPrevStatus(oldStatus);
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);
        bugOnlineBizDemandMapper.addRelations(bugOnlineId, bizDemandIds);

        BugLogDO bugLogDO = new BugLogDO()
                .setMainId(bugOnlineId)
                .setOldValue(oldStatusName)
                .setNewValue(BugOnlineStatusEnum.REQUIRED.getText())
                .setAction(ButtonActionEnum.ATTACH_BUSINESS.getText())
                .setField(BugLogFieldEnum.STATUS.getText())
                .setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogMapper.insert(bugLogDO);

        BugLogDO reasonBugLogDO = new BugLogDO()
                .setMainId(bugOnlineId)
                .setOldValue(oldReasonName)
                .setNewValue(BugOnlineReasonEnum.DEMAND_QUESTION.getText())
                .setField(BugLogFieldEnum.REASON.getText())
                .setType(BugLogTypeEnum.ONLINE.getCode());

        bugLogMapper.insert(reasonBugLogDO);

        // bug状态处理人员表插入数据
        bugLogComponent.insertToBugStatusOperator(bugOnlineId, userInfo.getId(), userInfo.getFullAlias());
        if (logToBizDemands) {
            // 业务需求变更日志
            addBizDemandAttachLogs(bugOnlineDO, bizDemandIds);
        }
    }

    private void addBizDemandAttachLogs(BugOnlineDO bugOnlineDO, Collection<Long> bizDemandIds) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<BizChangeLogDO> changeLogs = bizDemandIds.stream().map(id -> {
            BizChangeLogDO bizChangeLogDO = new BizChangeLogDO();
            bizChangeLogDO.setCreateManId(userInfo.getId());
            bizChangeLogDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            bizChangeLogDO.setType(BizChangeLogTypeEnum.BIZ_DEMAND.getCode())
                    .setMainId(id)
                    .setAction(ButtonActionEnum.LINK.getText())
                    .setField(BizChangeLogTypeEnum.ORIGIN_BUG_ONLINE.getText())
                    .setOldValue(bugOnlineDO.getName())
                    .setNewValue(bugOnlineDO.getName());
            return bizChangeLogDO;
        }).collect(Collectors.toList());
        bizChangeLogMapper.batchInsert(changeLogs);
    }

    private void addLog(Long bugOnlineId) {
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.AGREE.getText());
        bugLogDO.setOldValue(BugOnlineStatusEnum.BE_CONFIRM.getText());
        bugLogDO.setNewValue(BugOnlineStatusEnum.CLOSE.getText());
        bugLogDO.setMainId(bugOnlineId);
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        bugLogMapper.insert(bugLogDO);

        bugLogComponent.insertToBugStatusOperator(bugOnlineId, CommonConstant.SYSTEM, CommonConstant.SYSTEM);

        BugOnlineStatusOperatorDO bugOnlineStatusOperatorDO = new BugOnlineStatusOperatorDO();
        bugOnlineStatusOperatorDO.setBugOnlineId(bugOnlineId);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        bugOnlineStatusOperatorDO.setOperator(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        bugOnlineStatusOperatorDO.setStatus(BugOnlineStatusEnum.CLOSE.getCode());
        bugOnlineStatusOperatorDO.setOperatorId(userInfo.getId());
        bugOnlineStatusOperatorComponent.add(bugOnlineStatusOperatorDO);
    }

}
