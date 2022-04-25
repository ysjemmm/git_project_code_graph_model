package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectRiskExplanationMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.facade.api.client.ProjectRiskService;
import com.timevale.forward.facade.api.query.ProjectRiskQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskModifyReq;
import com.timevale.forward.facade.api.result.ProjectRiskVO;
import com.timevale.forward.model.enums.ProjectRiskStateEnum;
import com.timevale.forward.model.enums.ProjectRiskTypeEnum;
import com.timevale.forward.service.component.HomePageRiskWarningComponent;
import com.timevale.forward.service.component.HomePageRiskWarningSubmitTestComponent;
import com.timevale.forward.service.component.HomePageRiskWarningTaskComponent;
import com.timevale.forward.service.component.ProjectRiskExplanationComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectRiskCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RestService
public class ProjectRiskServiceImpl implements ProjectRiskService {

    @Resource
    ProjectRiskMapper projectRiskMapper;

    @Resource
    ProjectRiskExplanationComponent projectRiskExplanationComponent;

    @Resource
    HomePageRiskWarningComponent homePageRiskWarningComponent;

    @Resource
    HomePageRiskWarningSubmitTestComponent homePageRiskWarningSubmitTestComponent;

    @Resource
    HomePageRiskWarningTaskComponent homePageRiskWarningTaskComponent;


    @Override
    public BaseResult<Boolean> add(ProjectRiskAddReq projectRiskAddReq) {
        ProjectRiskDO riskDO = ProjectRiskCopier.INSTANCE.convert(projectRiskAddReq);
        // 类型为其它
        riskDO.setType(ProjectRiskTypeEnum.OTHER.getCode());
        riskDO.setSign("");
        projectRiskMapper.insert(riskDO);

        // 添加项目说明
        projectRiskExplanationComponent.add(riskDO.getId(), projectRiskAddReq.getExplanation());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(ProjectRiskModifyReq projectRiskModifyReq) {
        ProjectRiskDO riskDO = ProjectRiskCopier.INSTANCE.convert(projectRiskModifyReq);
        projectRiskMapper.update(riskDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectRiskVO> get(Long projectRiskId) {
        ProjectRiskDO riskDO = projectRiskMapper.selectById(projectRiskId);
        if(riskDO == null){
            throw new BaseBizRuntimeException("该项目风险不存在");
        }

        ProjectRiskVO riskVO = ProjectRiskCopier.INSTANCE.convert(riskDO);

        // 枚举填充
        riskVO.setTypeName(ProjectRiskTypeEnum.getTextByCode(riskVO.getType()));
        riskVO.setStateName(ProjectRiskStateEnum.getTextByCode(riskVO.getState()));

        return BaseResult.success(riskVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectRiskVO>> list(ProjectRiskQueryList projectRiskQueryList) {
        // 开始分页
        PageHelper.startPage(projectRiskQueryList.pageNum, projectRiskQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByProjectId(projectRiskQueryList.getProjectId());
        List<ProjectRiskVO> riskVOList = riskDOList.stream().map(ProjectRiskCopier.INSTANCE::convert).collect(Collectors.toList());

        // 枚举描述
        for (ProjectRiskVO e : riskVOList) {
            e.setTypeName(ProjectRiskTypeEnum.getTextByCode(e.getType()));
            e.setStateName(ProjectRiskStateEnum.getTextByCode(e.getState()));
        }

        // 返回分页数据
        PageInfo<ProjectRiskDO> pageInfo = new PageInfo<>(riskDOList);
        PageQueryResult<ProjectRiskVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(riskVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> sync() {

        return BaseResult.success(true);
    }
}
