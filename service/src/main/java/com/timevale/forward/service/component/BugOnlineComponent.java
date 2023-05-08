package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.model.enums.ButtonActionEnum;

import java.util.Collection;
import java.util.List;

public interface BugOnlineComponent {
    /**
     *
     * @param autoCloseLimitDay 多少天自动关闭
     */
    void autoCloseBugIfBeConfirm(int autoCloseLimitDay);

    /**
     * 自动钉钉通知关闭线上bug确认
     */
    void autoNoticeCloseBugIfBeConfirm(int autoCloseLimitDay);

    /**
     * 关联业务需求转需求
     */
    void attachToBizDemands(BugOnlineDO bugOnline, Collection<Long> bizDemandIds, ButtonActionEnum actionEnum);


    /**
     * 比较关联的线下bug
     *
     * @param bugOnlineId 线上bugid
     * @param oldId       旧的线下bugid
     * @param newId       新的线下bugid
     * @return bug日志实体
     */
    List<BugLogDO> compareBugOffline(Long bugOnlineId, Long oldId, Long newId);

    /**
     * 查询管理的业务域
     *
     * @param bugOnlineId 线上bug id
     * @return {@link List}<{@link BizDomainDO}>
     */
    List<BizDomainDO> getBizDomain(Long bugOnlineId);
}
