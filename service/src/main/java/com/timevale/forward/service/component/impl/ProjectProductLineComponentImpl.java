package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectProductLineMapper;
import com.timevale.forward.dal.entity.ProjectProductLineDO;
import com.timevale.forward.service.component.ProjectProductLineComponent;
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
        List<ProjectProductLineDO> projectProductLineDO = buildDO(list, projectId);
        log.info("项目-产品线新增接收参数:list={},projectId={}", list, projectId);
        List<ProjectProductLineDO> existProductLines = projectProductLineMapper.get(projectId);
        if(CollectionUtils.isEmpty(existProductLines)){
            // 新增 没找到 直接入库
            projectProductLineMapper.batchInsert(projectProductLineDO);
            return;
        }
        // 编辑
        List<Long> existProductLineIds = existProductLines.stream().map(ProjectProductLineDO::getProductLineId).collect(Collectors.toList());
        List<ProjectProductLineDO> needAddProductLines=new ArrayList<>();
        projectProductLineDO.forEach((f)->{
            if(!existProductLineIds.contains(f.getProductLineId())){
                needAddProductLines.add(f);
            }
        });
        projectProductLineMapper.batchInsert(needAddProductLines);
        log.info("新增项目-产品线:needAddProductLines={}", needAddProductLines);

//        List<String> reqFileIds = projectProductLineDO.stream().map(ProjectProductLineDO::getFileId).collect(Collectors.toList());
//        existFiles.forEach((f)->{
//            if(!reqFileIds.contains(f.getFileId())){
//                f.setIsDeleted(true);
//                //删除
//                fileMapper.update(f);
//            }
//        });
//        projectProductLineMapper.batchInsert(projectProductLineDOS);
    }

    private List<ProjectProductLineDO> buildDO(List<Long> list,Long projectId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        return list.stream().map(t -> {
            ProjectProductLineDO projectProductLineDO = new ProjectProductLineDO();
            projectProductLineDO.setProductLineId(t);
            projectProductLineDO.setProjectId(projectId);
            projectProductLineDO.setCreateMan(userInfo.getAlias());
            projectProductLineDO.setCreateManId(userInfo.getId());
            return projectProductLineDO;
        }).collect(Collectors.toList());
    }
}
