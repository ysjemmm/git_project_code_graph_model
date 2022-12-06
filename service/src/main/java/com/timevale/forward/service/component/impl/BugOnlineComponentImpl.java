package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.model.enums.BugLogFieldEnum;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.service.component.BugLogComponent;
import com.timevale.forward.service.component.BugOnlineComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.observer.event.BugOnlineConfirmMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.date.DateUtil;

import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

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



    @Override
    public void autoCloseBugIfBeConfirm(int autoCloseLimitDay) {
        log.info("待确认线上bug自动关闭-开始:{}",autoCloseLimitDay);
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
        log.info("待确认线上bug自动关闭-完成,更新id:{}",updateBugIds);
    }

    @Override
    public void autoNoticeCloseBugIfBeConfirm(int autoCloseLimitDay) {
        log.info("待确认线上bug自动关闭前钉钉通知-开始:{}",autoCloseLimitDay);
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
        if(CollectionUtils.isNotEmpty(noticeBugOnlies)){
            // 发送通知
            noticeBugOnlies.forEach(n-> messageEventPublisher.publish(new BugOnlineConfirmMsgEvent(
                    this,
                    n.getName(),
                   n.getProposerId(),
                    n.getId()
            )));
        }
        log.info("待确认线上bug自动关闭前钉钉通知-结束:{}",noticeIds);

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

        bugLogComponent.insertToBugStatusOperator(bugOnlineId,CommonConstant.SYSTEM,CommonConstant.SYSTEM);
    }

}
