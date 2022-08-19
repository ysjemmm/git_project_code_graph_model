package com.timevale.forward.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ManDayMapper;
import com.timevale.forward.dal.dao.ManDayReportMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.dal.entity.ManDayReportDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.ManDayReportService;
import com.timevale.forward.facade.api.query.ManDayReportQueryList;
import com.timevale.forward.facade.api.request.ManDayReportBatchApproveReq;
import com.timevale.forward.facade.api.request.ManDayReportModifyReq;
import com.timevale.forward.facade.api.result.ManDayReportListVO;
import com.timevale.forward.model.enums.AuditStatusEnum;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/08/19 16:27
 */
@Slf4j
@RestService
public class ManDayReportServiceImpl implements ManDayReportService {

    @Resource
    private ManDayMapper manDayMapper;
    @Resource
    private ManDayReportMapper manDayReportMapper;
    @Resource
    private ProjectMapper projectMapper;

    @Override
    public BaseResult<PageQueryResult<ManDayReportListVO>> page(ManDayReportQueryList manDayReportQueryList) {
        return null;
    }

    @Override
    public BaseResult<Boolean> modify(ManDayReportModifyReq manDayReportModifyReq) {
        Long id = manDayReportModifyReq.getId();

        ManDayReportDO manDayReportDO = manDayReportMapper.selectById(id);
        AssertUtil.notNull(manDayReportDO, "待提报不存在");

        Long manDayId = manDayReportDO.getManDayId();
        ManDayDO manDayDO = manDayMapper.getById(manDayId);
        AssertUtil.notNull(manDayReportDO, "对应人天不存在");

        Long projectId = manDayDO.getProjectId();
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO,"对应项目不存在");

        String pmId = projectDO.getPmId();
        String userId = LocalSessionUtils.getUserInfo().getId();
        AssertUtil.checkState(ObjectUtil.equal(pmId, userId), "您不是提报对应项目的项目经理，无权审批");

        Integer auditStatus = manDayReportModifyReq.getAuditStatus();
        AssertUtil.checkState(!AuditStatusEnum.AUDITING.getCode().equals(auditStatus), "审批状态只能变为审批通过和已驳回");

        ManDayReportDO updateDO = new ManDayReportDO();
        updateDO.setId(id);
        updateDO.setAuditStatus(auditStatus);

        // 判断审批状态
        if (AuditStatusEnum.APPROVE.getCode().equals(manDayReportModifyReq.getAuditStatus())) {
            manDayDO.setActualManDay(manDayReportDO.getAuditManDay());
            manDayMapper.updateActualManDay(manDayDO);
        } else {
            updateDO.setRejectReason(manDayReportDO.getRejectReason());
        }
        manDayReportMapper.updateById(updateDO);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> batchApprove(ManDayReportBatchApproveReq manDayReportBatchApproveReq) {
        String userId = LocalSessionUtils.getUserInfo().getId();

        List<Long> ids = manDayReportBatchApproveReq.getIds();
        List<ManDayReportDO> manDayReportDOS = manDayReportMapper.selectByIds(ids);

        // 对应人天数据
        List<Long> manDayIds = manDayReportDOS.stream().map(ManDayReportDO::getManDayId).collect(Collectors.toList());
        List<ManDayDO> manDayDOS = manDayMapper.getByIds(manDayIds);

        // 对应项目数据
        List<Long> projectIds = manDayDOS.stream().map(ManDayDO::getProjectId).distinct().collect(Collectors.toList());
        List<ProjectDO> projectDOS = projectMapper.getByIds(projectIds).stream().filter(e->!e.getIsDeleted()).collect(Collectors.toList());

        // 判断对应项目是否全为自己
        AssertUtil.checkState(projectDOS.stream().allMatch(e -> userId.equals(e.getPmId())), "您无法审批您不是项目经理的项目");

        Map<Long, BigDecimal> auditDayMap = manDayReportDOS.stream()
                .collect(Collectors.toMap(ManDayReportDO::getManDayId, ManDayReportDO::getAuditManDay, (a, b) -> a));

        log.info("[ManDayReportServiceImpl][batchApprove]批量更新人天{}",auditDayMap);
        for (Long id : manDayIds) {
            ManDayDO manDayDO = new ManDayDO();
            manDayDO.setId(id);
            manDayDO.setActualManDay(auditDayMap.get(id));
            manDayMapper.updateActualManDay(manDayDO);
        }
        manDayReportMapper.updateStatus(ids, AuditStatusEnum.APPROVE.getCode());

        return BaseResult.success(true);
    }
}
