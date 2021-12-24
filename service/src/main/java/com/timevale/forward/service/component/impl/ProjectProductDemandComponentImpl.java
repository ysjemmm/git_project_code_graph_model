package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectProductDemandComponentImpl implements ProjectProductDemandComponent {

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Override
    public void update(ProjectProductDemandDO projectProductDemandDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        projectProductDemandDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        projectProductDemandDO.setModifyManId(userInfo.getId());
        projectProductDemandMapper.update(projectProductDemandDO);
    }

    @Override
    public void batchInsert(Long projectId,List<Long> productDemandIds) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<ProjectProductDemandDO> list = productDemandIds.stream().map(i -> {
            ProjectProductDemandDO productDemandDO = new ProjectProductDemandDO();
            productDemandDO.setProductDemandId(i);
            productDemandDO.setProjectId(projectId);
            productDemandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            productDemandDO.setCreateManId(userInfo.getId());
            return productDemandDO;
        }).collect(Collectors.toList());
        projectProductDemandMapper.batchInsert(list);
    }

    @Override
    public ProjectProductDemandDO getByProjectId(Long projectId) {
        return projectProductDemandMapper.getByProjectId(projectId);
    }


}
