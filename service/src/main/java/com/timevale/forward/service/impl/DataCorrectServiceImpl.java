package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.BugOnlineStatusOperatorMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.dao.TestBillMapper;
import com.timevale.forward.dal.dao.TroubleTicketMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOnlineStatusOperatorDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.TestBillDO;
import com.timevale.forward.dal.entity.TroubleTicketDO;
import com.timevale.forward.facade.api.client.DataCorrectService;
import com.timevale.forward.facade.api.request.ProjectNodeModifyReq;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.ProjectNodeStatusEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BugOnlineStatusOperatorComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.annotation.RestService;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Resource
    private BugOnlineStatusOperatorMapper bugOnlineStatusOperatorMapper;

    @Resource
    private BugLogMapper bugLogMapper;

    @Resource
    private BugOnlineStatusOperatorComponent bugOnlineStatusOperatorComponent;


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

    @Override
    public BaseResult<Boolean> bugOnlineCloseStatusOperatorInit(Integer count) {
        List<Long> bugOnlineIds = bugOnlineStatusOperatorMapper.selectInitInfo(count);
        if(CollectionUtils.isEmpty(bugOnlineIds)){
            BaseResult.success(false);
        }
        bugOnlineIds.forEach(a->{
            List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(a, BugLogTypeEnum.ONLINE.getCode(), true)
                    .stream().filter(b -> BugOnlineStatusEnum.CLOSE.getText().equals(b.getNewValue())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bugLogDOList)) {
                bugLogDOList.sort(Comparator.comparing(BugLogDO::getCreateDate).reversed());
                BugLogDO bugLogDO = bugLogDOList.get(0);
                BugOnlineStatusOperatorDO bugOnlineStatusOperatorDO = new BugOnlineStatusOperatorDO();
                bugOnlineStatusOperatorDO.setBugOnlineId(a);
                bugOnlineStatusOperatorDO.setOperator(bugLogDO.getCreateMan());
                bugOnlineStatusOperatorDO.setStatus(BugOnlineStatusEnum.CLOSE.getCode());
                bugOnlineStatusOperatorDO.setOperatorId(bugLogDO.getCreateManId());
                bugOnlineStatusOperatorComponent.add(bugOnlineStatusOperatorDO);            }

        });
        return BaseResult.success(true);
    }

}
