package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProjectBudgetMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProjectBudgetDO;
import com.timevale.forward.facade.api.client.ProjectBudgetService;
import com.timevale.forward.facade.api.request.ProjectBudgetSaveReq;
import com.timevale.forward.facade.api.result.ProjectBudgetVO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.middle.ProjectBudgetMD;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectBudgetsCopier;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
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
    private BizChangeLogMapper bizChangeLogMapper;
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

            if (costAmount == null || BigDecimal.ZERO.compareTo(expectedAmount) >= 0) {
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

        // 日志
        bizChangeLogMapper.insert(createCommonChangeLog()
                .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setField(BizChangeLogFieldEnum.PJ_BUDGET.getText())
                .setMainId(req.getProjectId())
                .setIdentity(formIdentity(req.getCostType()))
                .setOldValue(req.getCostType())
                .setNewValue(req.getCostType())
                .setAction(ButtonActionEnum.PROJECT_BUDGET_ADD.getText())
        );
        return BaseResult.success(budgetVO);
    }

    @Override
    public BaseResult<ProjectBudgetVO> updateBudget(ProjectBudgetSaveReq req) {
        ProjectBudgetDO sameBudget = projectBudgetMapper.selectByCostType(req.getProjectId(), req.getCostType());
        AssertUtil.checkState(Objects.equals(sameBudget.getId(), req.getId()), "已有相同成本类型");

        ProjectBudgetDO oldLogBudget = projectBudgetMapper.selectById(req.getId());
        ProjectBudgetDO budgetDO = ProjectBudgetsCopier.INSTANCE.req2do(req);
        projectBudgetMapper.update(budgetDO);
        ProjectBudgetDO newLogBudget = projectBudgetMapper.selectById(req.getId());

        if (!Objects.equals(oldLogBudget.getCostType(), newLogBudget.getCostType())) {
            // 修改identity
            bizChangeLogMapper.updateIdentity(oldLogBudget.getProjectId(), formIdentity(oldLogBudget.getCostDesc()),
                    formIdentity(newLogBudget.getCostDesc()));
        }

        // 生成修改记录
        ProjectBudgetMD oldMd = ProjectBudgetsCopier.INSTANCE.do2md(oldLogBudget);
        ProjectBudgetMD newMd = ProjectBudgetsCopier.INSTANCE.do2md(newLogBudget);
        List<BizChangeLogDO> logs = FieldCompareUtil.commonCompare(oldMd, newMd, BizChangeLogDO.class);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        for (BizChangeLogDO log : logs) {
            log.setMainId(req.getProjectId());
            log.setIdentity(formIdentity(req.getCostType()));
            log.setCreateManId(userInfo.getId());
            log.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        }
        if (!logs.isEmpty()) {
            bizChangeLogMapper.batchInsert(logs);
        }

        ProjectBudgetVO budgetVO = ProjectBudgetsCopier.INSTANCE.do2vo(budgetDO);
        return BaseResult.success(budgetVO);
    }

    @Override
    public BaseResult<Void> deleteBudget(Long id) {
        // 日志
        ProjectBudgetDO budgetDO = projectBudgetMapper.selectById(id);
        // 日志
        bizChangeLogMapper.insert(createCommonChangeLog()
                .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setField(BizChangeLogFieldEnum.PJ_BUDGET.getText())
                .setMainId(budgetDO.getProjectId())
                .setIdentity(formIdentity(budgetDO.getCostType()))
                .setOldValue(budgetDO.getCostType())
                .setNewValue(budgetDO.getCostType())
                .setAction(ButtonActionEnum.PROJECT_BUDGET_DELETE.getText())
        );

        projectBudgetMapper.delete(id);
        return BaseResult.success();
    }

    private BizChangeLogDO createCommonChangeLog() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        BizChangeLogDO bizChangeLogDO = new BizChangeLogDO();
        bizChangeLogDO.setCreateManId(userInfo.getId());
        bizChangeLogDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        return bizChangeLogDO;
    }

    private String formIdentity(String name) {
        return BizChangeLogFieldEnum.PJ_BUDGET.getText() + CommonConstant.WIDE_COLON + name;
    }
}
