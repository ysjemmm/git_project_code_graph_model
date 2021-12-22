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


}
