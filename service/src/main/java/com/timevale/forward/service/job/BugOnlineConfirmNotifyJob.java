package com.timevale.forward.service.job;

import com.timevale.forward.service.component.BugOnlineComponent;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;

import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 线上bug待确认关闭通知
 * @ClassName: BugOnlineConfirmNotifyJob
 * @Author yexuan
 * @Date  2022-12-06 11:09
 */
@Slf4j
@JobHandler(value = "bugOnlineConfirmNotifyJob")
public class BugOnlineConfirmNotifyJob extends IJobHandler {


    @Value("${auto.confirm.limit:7}")
    private Integer autoConfirmLimitDay;

    @Resource
    private BugOnlineComponent bugOnlineComponent;

    @Override
    public ReturnT<String> execute(String s) {
        autoConfirmLimitDay = 1;
        bugOnlineComponent.autoNoticeCloseBugIfBeConfirm(autoConfirmLimitDay-1);
        return ReturnT.SUCCESS;
    }
}
