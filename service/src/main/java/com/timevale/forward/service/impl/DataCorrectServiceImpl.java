package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.DataCorrectService;
import com.timevale.forward.facade.api.request.ProjectNodeModifyReq;
import com.timevale.forward.model.enums.ProjectNodeStatusEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class DataCorrectServiceImpl implements DataCorrectService {

    @Resource
    private ProjectComponent projectComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private TestBillMapper testBillMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProjectNodeComponent projectNodeComponent;
    @Resource
    private TroubleTicketMapper troubleTicketMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> nodeStatusUpdate() {
        List<Long> projectIdList = projectMapper.getAllId();

        projectIdList.forEach(e -> {
            // 查询项目节点
            List<ProjectNodeDO> nodeDOList = projectNodeComponent.get(e);

            // 如果节点为空则状态设为待启动
            Integer nodeStatus;
            if (org.apache.commons.collections.CollectionUtils.isEmpty(nodeDOList)) {
                nodeStatus = ProjectNodeStatusEnum.READY_START.getCode();
            } else {
                nodeStatus = projectNodeComponent.getStatus(nodeDOList);
            }
            // 更新项目节点状态
            ProjectDO projectDO = new ProjectDO();
            projectDO.setId(e);
            projectDO.setNodeStatus(nodeStatus);
            projectMapper.updateNodeStatus(projectDO);
        });

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> bizDemandProjectEndDateUpdate() {
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.selectAll();
        bizDemandDOList = bizDemandDOList.stream().filter(e -> !e.getIsDeleted()).collect(Collectors.toList());

        for (BizDemandDO e : bizDemandDOList) {
            Date projectEndDate = bizDemandComponent.getProjectEndDate(e.getId());
            if (projectEndDate != null) {
                bizDemandMapper.updateDate(e.getId(), projectEndDate, DateUtil.getMonth(projectEndDate) - 1);
            }
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateNodeDate(ProjectNodeModifyReq req) {
        projectNodeMapper.updateActualDateById(req.getId(),req.getActualDate());
        ProjectNodeDO projectNodeDO = projectNodeMapper.getById(req.getId());
        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setProjectId(projectNodeDO.getProjectId());
        testBillDO.setDelayDay(0);
        testBillMapper.updateDelayDay(testBillDO,true);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> troubleTicketTime() {
        List<TroubleTicketDO> troubleTicketDOS = troubleTicketMapper.selectAll();

        // 只保存仅有的时间
        troubleTicketDOS = troubleTicketDOS.stream()
                .filter(e -> e.getOccurrenceTime() != null && e.getRestoreTime() != null)
                .collect(Collectors.toList());

        for (TroubleTicketDO e : troubleTicketDOS) {
            Date occurrenceTime = e.getOccurrenceTime();
            Date restoreTime = e.getRestoreTime();

            long occurrenceTimeTime = occurrenceTime.getTime();
            long restoreTimeTime = restoreTime.getTime();

            long differ = Math.max(restoreTimeTime - occurrenceTimeTime, 0L);
            long result = differ / DateFormatConst.ONE_MINUTE;
            BigDecimal durationTime = BigDecimal.valueOf(result);

            log.info("[DataCorrectServiceImpl][troubleTicketTime]更新故障单{}持续时间{}", e.getId(), durationTime);
            troubleTicketMapper.updateDurationTime(e.getId(), durationTime);
        }
        return BaseResult.success(true);
    }

}
