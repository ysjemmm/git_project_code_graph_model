package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectRiskExplanationMapper;
import com.timevale.forward.dal.entity.ProjectRiskExplanationDO;
import com.timevale.forward.facade.api.client.ProjectRiskExplanationService;
import com.timevale.forward.facade.api.query.ProjectRiskExplanationQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskExplanationAddReq;
import com.timevale.forward.facade.api.result.ProjectRiskExplanationVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectRiskExplanationCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestService
public class ProjectRiskExplanationServiceImpl implements ProjectRiskExplanationService {

    @Resource
    ProjectRiskExplanationMapper projectRiskExplanationMapper;

    @Override
    public BaseResult<Boolean> add(ProjectRiskExplanationAddReq explanationAddReq) {
        ProjectRiskExplanationDO explanationDO = ProjectRiskExplanationCopier.INSTANCE.convert(explanationAddReq);
        projectRiskExplanationMapper.insert(explanationDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectRiskExplanationVO>> list(ProjectRiskExplanationQueryList explanationQueryList) {
        // 开始分页
        PageHelper.startPage(explanationQueryList.pageNum, explanationQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        List<ProjectRiskExplanationDO> explanationDOList = projectRiskExplanationMapper.selectByProjectRiskId(explanationQueryList.getProjectRiskId());
        List<ProjectRiskExplanationVO> explanationVOList = explanationDOList.stream().map(ProjectRiskExplanationCopier.INSTANCE::convert).collect(Collectors.toList());

        // 返回分页数据
        PageInfo<ProjectRiskExplanationDO> pageInfo = new PageInfo<>(explanationDOList);
        PageQueryResult<ProjectRiskExplanationVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(explanationVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }
}
