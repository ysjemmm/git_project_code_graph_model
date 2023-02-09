package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectIncomeMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectIncomeDO;
import com.timevale.forward.facade.api.client.ProjectIncomeService;
import com.timevale.forward.facade.api.request.ProjectIncomeSaveReq;
import com.timevale.forward.facade.api.result.ProjectIncomeDetailVO;
import com.timevale.forward.facade.api.result.ProjectIncomeVO;
import com.timevale.forward.service.copy.ProjectIncomeCopier;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


/**
 * @author by YangXu
 * @date 2023/02/06 16:40
 */
@Slf4j
@RestService
public class ProjectIncomeServiceImpl implements ProjectIncomeService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectIncomeMapper projectIncomeMapper;

    @Override
    public BaseResult<ProjectIncomeVO> addIncome(ProjectIncomeSaveReq req) {
        // 判断关联项目是否存在
        Long projectId = req.getProjectId();
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        // 转换，落库
        ProjectIncomeDO projectIncomeDO = ProjectIncomeCopier.INSTANCE.req2Do(req);
        projectIncomeMapper.insert(projectIncomeDO);

        // 转换，返回数据
        ProjectIncomeVO projectIncomeVO = ProjectIncomeCopier.INSTANCE.do2Vo(projectIncomeDO);
        return BaseResult.success(projectIncomeVO);
    }

    @Override
    public BaseResult<ProjectIncomeVO> updateIncome(ProjectIncomeSaveReq req) {
        // 转换，落库
        ProjectIncomeDO projectIncomeDO = ProjectIncomeCopier.INSTANCE.req2Do(req);
        projectIncomeMapper.update(projectIncomeDO);

        // 转换，返回数据
        ProjectIncomeVO projectIncomeVO = ProjectIncomeCopier.INSTANCE.do2Vo(projectIncomeDO);
        return BaseResult.success(projectIncomeVO);
    }

    @Override
    public BaseResult<Void> deleteIncome(Long id) {
        projectIncomeMapper.delete(id);
        return BaseResult.success();
    }

    @Override
    public BaseResult<ProjectIncomeDetailVO> listIncome(Long projectId) {
        // 判断关联项目是否存在
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        // 查询对应项目下的项目收益
        List<ProjectIncomeDO> projectIncomeDOs = projectIncomeMapper.selectByProjectId(projectId);

        // 已收益金额总额
        BigDecimal incomeAmountSum = BigDecimal.ZERO;
        for (ProjectIncomeDO e : projectIncomeDOs) {
            incomeAmountSum = incomeAmountSum.add(e.getIncomeAmount());
        }

        // 项目预计收益金额
        BigDecimal expectedIncome = projectDO.getExpectedIncome();

        // 计算收益进度：已收益金额的累计值/预计收益金额*100%,四舍五入
        BigDecimal progress = BigDecimal.ZERO;
        if (BigDecimal.ZERO.compareTo(expectedIncome) <= 0) {
            progress = incomeAmountSum.divide(expectedIncome,4, RoundingMode.HALF_UP);
        }

        // 组合结果,返回
        List<ProjectIncomeVO> projectIncomeVOs = ProjectIncomeCopier.INSTANCE.do2Vo(projectIncomeDOs);

        ProjectIncomeDetailVO result = new ProjectIncomeDetailVO();
        result.setProgress(progress);
        result.setExpectedIncome(expectedIncome);
        result.setIncomes(projectIncomeVOs);

        return BaseResult.success(result);
    }
}
