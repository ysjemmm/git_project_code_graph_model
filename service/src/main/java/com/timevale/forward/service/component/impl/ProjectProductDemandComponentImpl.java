package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public void update(Long projectId,Long productDemandId) {
        ProjectProductDemandDO projectProductDemandDO=new ProjectProductDemandDO();
        projectProductDemandDO.setProjectId(projectId);
        projectProductDemandDO.setProductDemandId(productDemandId);
        projectProductDemandDO.setIsDeleted(true);
        projectProductDemandMapper.update(projectProductDemandDO);
    }

    @Override
    public void batchInsert(Long projectId, List<Long> productDemandIds) {
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectId);
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        log.info("关联产品需求,existProductDemandIds={}", existProductDemandIds);
        productDemandIds.removeAll(existProductDemandIds);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if(!CollectionUtils.isEmpty(productDemandIds)){
            Set<Long> set = new HashSet<>(productDemandIds);
            List<ProjectProductDemandDO> list = set.stream().map(i -> {
                ProjectProductDemandDO productDemandDO = new ProjectProductDemandDO();
                productDemandDO.setProductDemandId(i);
                productDemandDO.setProjectId(projectId);
                productDemandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
                productDemandDO.setCreateManId(userInfo.getId());
                return productDemandDO;
            }).collect(Collectors.toList());
            projectProductDemandMapper.batchInsert(list);
        }

    }


}
