package com.timevale.forward.service.impl;

import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.facade.api.client.PublishPlanService;
import com.timevale.forward.facade.api.query.ProjectLinkPublishPlanQueryList;
import com.timevale.forward.facade.api.query.PublishPlanQueryList;
import com.timevale.forward.facade.api.request.ProjectPublishPlanLinkReq;
import com.timevale.forward.facade.api.result.PublishPlanVO;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
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
public class PublishPlanServiceImpl implements PublishPlanService {

    @Resource
    private EpeiusClient epeiusClient;

    @Override
    public BaseResult<PageQueryResult<PublishPlanVO>> matchPublishPlan(PublishPlanQueryList publishPlanQueryList) {
        List<PublishPlanVO> publishPlans = Lists.newArrayList();
        // 返回分页数据
        PageInfo<BizDemandListDO> pageInfo = new PageInfo<>();
        PageQueryResult<PublishPlanVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(publishPlans);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> linkOrUnLinkPublishPlan(ProjectPublishPlanLinkReq projectPublishPlanLinkReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<PublishPlanVO>> linkPublishPlanList(ProjectLinkPublishPlanQueryList projectLinkPublishPlanQueryList) {
        List<PublishPlanVO> publishPlans = Lists.newArrayList();
        // 返回分页数据
        PageInfo<BizDemandListDO> pageInfo = new PageInfo<>();
        PageQueryResult<PublishPlanVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(publishPlans);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }
}
