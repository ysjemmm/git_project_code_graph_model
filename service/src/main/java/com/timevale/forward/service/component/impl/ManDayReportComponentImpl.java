package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.ManDayMapper;
import com.timevale.forward.dal.dao.ManDayReportMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.dal.entity.ManDayReportDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.model.enums.AuditStatusEnum;
import com.timevale.forward.service.component.ManDayReportComponent;
import com.timevale.forward.service.observer.event.ManDayReportAddMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/08/22 10:17
 */
@Slf4j
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
    public void add(Long manDayId, BigDecimal auditManDay, String auditManDayDesc) {

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ManDayDO manDayDO = manDayMapper.getById(manDayId);
        ProjectDO projectDO = projectMapper.get(manDayDO.getProjectId());
        boolean isPM = projectDO.getPmId().equals(userInfo.getId());

        ManDayReportDO reportDO = new ManDayReportDO()
                .setManDayId(manDayId)
                .setAuditManDay(auditManDay)
                .setManDayDesc(auditManDayDesc)
                .setAuditStatus(isPM ? AuditStatusEnum.APPROVE.getCode() : AuditStatusEnum.AUDITING.getCode())
                .setAuditor(projectDO.getPmName())
                .setAuditorId(projectDO.getPmId())
                .setReportor(manDayDO.getMemberName())
                .setReportorId(manDayDO.getMemberId());
        manDayReportMapper.insert(reportDO);

        // 发送消息
        if (!isPM) {
            messageEventPublisher.publish(new ManDayReportAddMsgEvent(
                    this,
                    userInfo.getAlias() + "-" + userInfo.getName(),
                    reportDO.getAuditorId(),
                    DateUtil.formDateRange(manDayDO.getWeekStartDate(), manDayDO.getWeekEndDate()),
                    projectDO.getName(),
                    auditManDay.toString()
            ));
        }
    }

    @Override
    public void updateAuditor(Long projectId) {
        // 查询对应项目
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            return;
        }

        // 查询对应项目审核中的人天
        List<ManDayDO> manDayDOs = manDayMapper.getByProjectId(projectId);
        List<Long> manDayIds = manDayDOs.stream()
                .filter(e -> Objects.equals(e.getAuditStatus(), AuditStatusEnum.AUDITING.getCode()))
                .map(BaseDO::getId)
                .collect(Collectors.toList());
        if (CollectionUtil.isEmpty(manDayIds)) {
            return;
        }

        // 查询对应人天最新的提报
        List<ManDayReportDO> reportDOs = manDayReportMapper.selectByManDayIdsLast(manDayIds);
        List<Long> reportIds = reportDOs.stream().map(BaseDO::getId).collect(Collectors.toList());

        manDayReportMapper.updateAuditor(reportIds, projectDO.getPmName(), projectDO.getPmId());
    }

    @Override
    public void batchMsg(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        List<ManDayDO> manDayDOS = manDayMapper.getByProjectId(projectId);
        List<Long> manDayIds = manDayDOS.stream().map(BaseDO::getId).collect(Collectors.toList());

        if (CollectionUtil.isEmpty(manDayIds)) {
            log.info("[ManDayReportComponentImpl][batchMsg]对应项目没有人天:{}", projectId);
            return;
        }

        Map<Long, ManDayDO> manDayDOMap = manDayDOS.stream().collect(Collectors.toMap(BaseDO::getId, Function.identity()));

        // 查询每个人天最新一条提报
        List<ManDayReportDO> reportDOS = manDayReportMapper.selectByManDayIdsLast(manDayIds);
        // 只筛选审核中的提报
        reportDOS = reportDOS.stream().filter(e -> Objects.equals(AuditStatusEnum.AUDITING.getCode(), e.getAuditStatus())).collect(Collectors.toList());

        log.info("[ManDayReportComponentImpl][batchMsg]批量发送提报审核信息");
        for (ManDayReportDO reportDO : reportDOS) {
            ManDayDO manDayDO = manDayDOMap.get(reportDO.getManDayId());
            messageEventPublisher.publish(new ManDayReportAddMsgEvent(
                    this,
                    reportDO.getCreateMan(),
                    projectDO.getPmId(),
                    DateUtil.formDateRange(manDayDO.getWeekStartDate(), manDayDO.getWeekEndDate()),
                    projectDO.getName(),
                    reportDO.getAuditManDay().toString()
            ));
        }
    }
}
