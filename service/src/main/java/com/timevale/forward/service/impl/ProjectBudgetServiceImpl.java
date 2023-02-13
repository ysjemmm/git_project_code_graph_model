package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectBudgetMapper;
import com.timevale.forward.dal.entity.ProjectBudgetDO;
import com.timevale.forward.facade.api.client.ProjectBudgetService;
import com.timevale.forward.facade.api.request.ProjectBudgetSaveReq;
import com.timevale.forward.facade.api.result.ProjectBudgetVO;
import com.timevale.forward.service.copy.ProjectBudgetsCopier;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestService
public class ProjectBudgetServiceImpl implements ProjectBudgetService {

    @Resource
    private ProjectBudgetMapper projectBudgetMapper;

    @Override
    public BaseResult<List<ProjectBudgetVO>> listBudgets(Long projectId) {
        // 查询后转换
        List<ProjectBudgetDO> budgetDOs = projectBudgetMapper.selectByProjectId(projectId);
        List<ProjectBudgetVO> budgetVOs = ProjectBudgetsCopier.INSTANCE.do2vo(budgetDOs);

        // 执行进度：实际已发生成本金额/预计成本金额*100%
        for (ProjectBudgetVO e : budgetVOs) {
            BigDecimal costAmount = e.getCostAmount();
            BigDecimal expectedAmount = e.getExpectedAmount();

            if (BigDecimal.ZERO.compareTo(expectedAmount) >= 0) {
                e.setProgress(BigDecimal.ZERO);
            } else {
                BigDecimal progress = costAmount
                        .multiply(BigDecimal.valueOf(100))
                        .divide(expectedAmount, 2, RoundingMode.HALF_UP);
                e.setProgress(progress);
            }
        }
        return BaseResult.success(budgetVOs);
    }

    @Override
    public BaseResult<ProjectBudgetVO> addBudget(ProjectBudgetSaveReq req) {
        ProjectBudgetDO sameBudget = projectBudgetMapper.selectByCostType(req.getProjectId(), req.getCostType());
        AssertUtil.checkState(sameBudget == null, "已有相同成本类型");

        ProjectBudgetDO budgetDO = ProjectBudgetsCopier.INSTANCE.req2do(req);
        projectBudgetMapper.insert(budgetDO);

        ProjectBudgetVO budgetVO = ProjectBudgetsCopier.INSTANCE.do2vo(budgetDO);
        return BaseResult.success(budgetVO);
    }

    @Override
    public BaseResult<ProjectBudgetVO> updateBudget(ProjectBudgetSaveReq req) {
        ProjectBudgetDO sameBudget = projectBudgetMapper.selectByCostType(req.getProjectId(), req.getCostType());
        AssertUtil.checkState(Objects.equals(sameBudget.getId(), req.getId()), "已有相同成本类型");

        ProjectBudgetDO budgetDO = ProjectBudgetsCopier.INSTANCE.req2do(req);
        projectBudgetMapper.update(budgetDO);

        ProjectBudgetVO budgetVO = ProjectBudgetsCopier.INSTANCE.do2vo(budgetDO);
        return BaseResult.success(budgetVO);
    }

    @Override
    public BaseResult<Void> deleteBudget(Long id) {
        projectBudgetMapper.delete(id);
        return BaseResult.success();
    }
}
