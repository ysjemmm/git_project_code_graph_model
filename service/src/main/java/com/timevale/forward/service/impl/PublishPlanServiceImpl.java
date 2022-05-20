package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectPublishPlanMapper;
import com.timevale.forward.dal.dto.PublishPlanDTO;
import com.timevale.forward.dal.dto.PublishPlanResultDTO;
import com.timevale.forward.dal.entity.ProjectPublishPlanDO;
import com.timevale.forward.facade.api.client.PublishPlanService;
import com.timevale.forward.facade.api.query.PublishPlanQueryList;
import com.timevale.forward.facade.api.request.ProjectPublishPlanLinkReq;
import com.timevale.forward.facade.api.result.PublishPlanVO;
import com.timevale.forward.model.enums.ApproveStatusEnum;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.PublishStatusEnum;
import com.timevale.forward.service.component.ProjectPublishPlanComponent;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class PublishPlanServiceImpl implements PublishPlanService {

    @Resource
    private PublishPlatformClient publishPlatformClient;

    @Resource
    private ProjectPublishPlanMapper projectPublishPlanMapper;

    @Resource
    private ProjectPublishPlanComponent projectPublishPlanComponent;

    @Override
    public BaseResult<PageQueryResult<PublishPlanVO>> matchPublishPlan(PublishPlanQueryList publishPlanQueryList) {
        PublishPlanResultDTO resultDTO = publishPlatformClient.list(publishPlanQueryList);
        List<Long> publishPlanIds = projectPublishPlanMapper.get(publishPlanQueryList.getProjectId())
                .stream().map(ProjectPublishPlanDO::getPublishPlanId).collect(Collectors.toList());
        List<PublishPlanVO> publishPlans = Lists.newArrayList();
        List<PublishPlanDTO> list = resultDTO.getList();
        if(CollectionUtils.isEmpty(list)){
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        list.forEach(a -> {
            PublishPlanVO publishPlanVO = new PublishPlanVO();
            publishPlanVO.setAppNames(a.getApps());
            publishPlanVO.setName(a.getName());
            publishPlanVO.setCreateMan(a.getCreatePerson());
            publishPlanVO.setId(a.getId());
            publishPlanVO.setEmergency(a.getEmergency());
            publishPlanVO.setWindowStart(a.getWindowStart());
            publishPlanVO.setWindowEnd(a.getWindowEnd());
            publishPlanVO.setReleaseStatus(PublishStatusEnum.getTextByName(a.getReleaseStatus()));
            publishPlanVO.setStatus(ApproveStatusEnum.getTextByName(a.getStatus()));
            publishPlanVO.setIsLinked(publishPlanIds.contains(a.getId()));
            publishPlans.add(publishPlanVO);
        });
        Integer count = resultDTO.getCount();
        int pageSize = publishPlanQueryList.getPageSize();
        PageQueryResult<PublishPlanVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(publishPlans);
        pageQueryResult.setTotalItems(count);
        pageQueryResult.setTotalPages(count % pageSize == 0 ? count / pageSize : (count / pageSize) + 1);
        pageQueryResult.setCurrentPage(publishPlanQueryList.getPageNum());
        pageQueryResult.setItemsPerPage(publishPlanQueryList.getPageSize());
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> linkOrUnLinkPublishPlan(ProjectPublishPlanLinkReq projectPublishPlanLinkReq) {
        List<Long> publishPlanIds = projectPublishPlanLinkReq.getPublishPlanId();
        Long projectId = projectPublishPlanLinkReq.getProjectId();
        if (LinkOrUnLinkEnum.LINK.getCode().equals(projectPublishPlanLinkReq.getType())) {
            projectPublishPlanComponent.add(publishPlanIds,projectId);
        }else {
            projectPublishPlanComponent.update(publishPlanIds.get(0),projectId);
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<PublishPlanVO>> linkPublishPlanList(PublishPlanQueryList publishPlanQueryList) {
        Long projectId = publishPlanQueryList.getProjectId();
        List<Long> publishPlanIds = projectPublishPlanMapper.get(projectId)
                .stream()
                .map(ProjectPublishPlanDO::getPublishPlanId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(publishPlanIds)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        publishPlanQueryList.setId(StringUtils.join( publishPlanIds,","));
        return matchPublishPlan(publishPlanQueryList);
    }
}
