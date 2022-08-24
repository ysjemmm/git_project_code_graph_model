package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ManDayMapper;
import com.timevale.forward.dal.dao.ManDayReportMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.dal.entity.ManDayReportDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.model.enums.AuditStatusEnum;
import com.timevale.forward.service.component.ManDayReportComponent;
import com.timevale.forward.service.observer.event.ManDayReportAddMsgEvent;
import com.timevale.forward.service.observer.event.ManDayReportApproveMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2022/08/22 10:17
 */
@Component
public class ManDayReportComponentImpl implements ManDayReportComponent {

    @Resource
    private ManDayReportMapper manDayReportMapper;
    @Resource
    private ManDayMapper manDayMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Override
    public void add(Long manDayId, BigDecimal auditManDay) {
        manDayReportMapper.insert(new ManDayReportDO()
                .setManDayId(manDayId)
                .setAuditManDay(auditManDay)
                .setAuditStatus(AuditStatusEnum.AUDITING.getCode()));
        // 发送消息
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ManDayDO manDayDO = manDayMapper.getById(manDayId);
        ProjectDO projectDO = projectMapper.get(manDayDO.getProjectId());

        messageEventPublisher.publish(new ManDayReportAddMsgEvent(
                this,
                userInfo.getAlias() + "-" + userInfo.getName(),
                manDayDO.getMemberId(),
                DateUtil.formDateRange(manDayDO.getWeekStartDate(), manDayDO.getWeekEndDate()),
                projectDO.getName(),
                auditManDay.toString()
        ));
    }
}
