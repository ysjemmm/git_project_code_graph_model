package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectAcceptanceListCondition;
import com.timevale.forward.dal.dao.ProjectAcceptanceMapper;
import com.timevale.forward.dal.entity.ProjectAcceptanceDO;
import com.timevale.forward.facade.api.client.ProjectAcceptanceService;
import com.timevale.forward.facade.api.query.ProjectAcceptanceQueryList;
import com.timevale.forward.facade.api.request.ProjectAcceptanceAddReq;
import com.timevale.forward.facade.api.request.ProjectAcceptanceModifyReq;
import com.timevale.forward.facade.api.result.ProjectAcceptanceVO;
import com.timevale.forward.model.enums.FlowStatusEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectAcceptanceCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProjectAcceptanceServiceImpl implements ProjectAcceptanceService {


    @Resource
    private ProjectAcceptanceMapper projectAcceptanceMapper;


    @Override
    public BaseResult<List<ProjectAcceptanceVO>> list(ProjectAcceptanceQueryList query) {
        List<Integer> status = Lists.newArrayList(FlowStatusEnum.AUDITING.getCode(), FlowStatusEnum.COMPLETE.getCode(), FlowStatusEnum.REJECT.getCode());
        ProjectAcceptanceListCondition c = ProjectAcceptanceCopier.INSTANCE.convert(query);
        c.setStatus(status);
        PageHelper.startPage(query.getPageNum(), query.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProjectAcceptanceDO> list = projectAcceptanceMapper.list(c);

        List<ProjectAcceptanceVO> projectAcceptanceVOList = ProjectAcceptanceCopier.INSTANCE.convert(list);

        return BaseResult.success(projectAcceptanceVOList);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectAcceptanceVO>> history(ProjectAcceptanceQueryList query) {
        ProjectAcceptanceListCondition c = ProjectAcceptanceCopier.INSTANCE.convert(query);
        PageHelper.startPage(query.getPageNum(), query.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProjectAcceptanceDO> list = projectAcceptanceMapper.list(c);

        List<ProjectAcceptanceVO> projectAcceptanceVOList = ProjectAcceptanceCopier.INSTANCE.convert(list);

        PageInfo<ProjectAcceptanceDO> pageInfo = new PageInfo<>(list);
        PageQueryResult<ProjectAcceptanceVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(projectAcceptanceVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> add(ProjectAcceptanceAddReq req) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> accept(ProjectAcceptanceModifyReq req) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> unAccept(ProjectAcceptanceModifyReq req) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> remind(Long id) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> revoke(Long id) {
        return BaseResult.success(true);
    }
}
