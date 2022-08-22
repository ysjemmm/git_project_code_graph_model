package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ManDayReportMapper;
import com.timevale.forward.dal.entity.ManDayReportDO;
import com.timevale.forward.model.enums.AuditStatusEnum;
import com.timevale.forward.service.component.ManDayReportComponent;
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

    @Override
    public void add(Long manDayId, BigDecimal auditManDay) {
        manDayReportMapper.insert(new ManDayReportDO()
                .setManDayId(manDayId)
                .setAuditManDay(auditManDay)
                .setAuditStatus(AuditStatusEnum.AUDITING.getCode()));
    }
}
