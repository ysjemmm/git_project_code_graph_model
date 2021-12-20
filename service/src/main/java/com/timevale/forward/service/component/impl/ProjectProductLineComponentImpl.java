package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectProductLineMapper;
import com.timevale.forward.dal.entity.ProjectProductLineDO;
import com.timevale.forward.service.component.ProjectProductLineComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectProductLineComponentImpl implements ProjectProductLineComponent {

    @Resource
    private ProjectProductLineMapper projectProductLineMapper;

    @Override
    public void add(List<Long> list,Long projectId) {
        log.info("项目-产品线新增接收参数:list={},projectId={}", list, projectId);
        List<ProjectProductLineDO> existProductLines = projectProductLineMapper.get(projectId);
        log.info("已存在项目-产品线:existProductLines={}", existProductLines);
        if(CollectionUtils.isEmpty(existProductLines)){
            List<ProjectProductLineDO> projectProductLineDO = buildDO(list, projectId);
            projectProductLineMapper.batchInsert(projectProductLineDO);
        }
    }

    @Override
    public void update(List<Long> list, Long projectId) {
        List<ProjectProductLineDO> projectProductLineDO = buildDO(list, projectId);
        log.info("项目-产品线编辑接收参数:list={},projectId={}", list, projectId);
        List<ProjectProductLineDO> existProductLines = projectProductLineMapper.get(projectId);
        List<Long> existProductLineIds = existProductLines.stream().map(ProjectProductLineDO::getProductLineId).collect(Collectors.toList());
        log.info("已存在项目-产品线:existProductLines={}", existProductLines);
        List<ProjectProductLineDO> needAddProductLines=new ArrayList<>();
        projectProductLineDO.forEach((f)->{
            if(!existProductLineIds.contains(f.getProductLineId())){
                needAddProductLines.add(f);
            }
        });
        if(CollectionUtils.isNotEmpty(needAddProductLines)){
            projectProductLineMapper.batchInsert(needAddProductLines);
            log.info("新增项目-产品线:needAddProductLines={}", needAddProductLines);
        }

        List<Long> reqProductIds = projectProductLineDO.stream().map(ProjectProductLineDO::getProductLineId).collect(Collectors.toList());
        existProductLines.forEach((p)->{
            if(!reqProductIds.contains(p.getProductLineId())){
                UserInfo userInfo = LocalSessionUtils.getUserInfo();
                p.setIsDeleted(true);
                p.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
                p.setModifyManId(userInfo.getId());
                //删除
                projectProductLineMapper.update(p);
            }
        });
    }

    @Override
    public List<Long> get(Long projectId) {
        List<Long> productLineIds =  projectProductLineMapper.get(projectId)
                .stream()
                .map(ProjectProductLineDO::getProductLineId).collect(Collectors.toList());
        return productLineIds;
    }

    private List<ProjectProductLineDO> buildDO(List<Long> list,Long projectId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        return list.stream().map(t -> {
            ProjectProductLineDO projectProductLineDO = new ProjectProductLineDO();
            projectProductLineDO.setProductLineId(t);
            projectProductLineDO.setProjectId(projectId);
            projectProductLineDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            projectProductLineDO.setCreateManId(userInfo.getId());
            return projectProductLineDO;
        }).collect(Collectors.toList());
    }
}
