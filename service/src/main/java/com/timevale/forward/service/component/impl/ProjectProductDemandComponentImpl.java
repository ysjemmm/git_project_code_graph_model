package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
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
    private FileMapper fileMapper;

    @Override
    public void update(ProjectProductDemandDO projectProductDemandDO) {
    }


}
