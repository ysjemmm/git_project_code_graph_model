package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.ProjectMD;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProjectLogComponentImpl implements ProjectLogComponent {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private PersonComponent personComponent;

    @Override
    public void addLogWhenModifyData(ProjectDO oldObj, ProjectDO newObj) {
        //{操作人} 把{字段名称} 从{原内容}改为{最新内容}
        ProjectMD oldProject = ProjectCopier.INSTANCE.change(oldObj);
        ProjectMD newProject = ProjectCopier.INSTANCE.change(newObj);
        List<BizChangeLogDO> logs = FieldCompareUtil.commonCompare(oldProject, newProject, BizChangeLogDO.class);
        if (!CollectionUtil.isEqualList(oldObj.getProductLineIds(), newObj.getProductLineIds())) {
            List<Long> productLineIds = new ArrayList<>(oldObj.getProductLineIds());
            productLineIds.addAll(newObj.getProductLineIds());

            Map<Long, String> productLineMap = productLineMapper.selectByIds(productLineIds)
                    .stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (v1, v2) -> v2));
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
            logDO.setMainId(oldObj.getId());
            logDO.setField(BizChangeLogFieldEnum.PRODUCT_LINE.getText());
            String oldValue = oldObj.getProductLineIds().stream().map(productLineMap::get).collect(Collectors.joining(","));
            String newValue = newObj.getProductLineIds().stream().map(productLineMap::get).collect(Collectors.joining(","));
            logDO.setOldValue(oldValue);
            logDO.setNewValue(newValue);
            logs.add(logDO);
        }


        Map<String, String> oldPds = personComponent.select(oldObj.getId(), PersonTypeEnum.PROJECT_PD.getCode())
                .stream().collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName));
        Map<String, String> newPds = newObj.getPds().stream().collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName, (v1, v2) -> v2));
        if (!CollectionUtil.isEqualList(oldPds.keySet(), newPds.keySet())) {
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
            logDO.setMainId(oldObj.getId());
            logDO.setField(BizChangeLogFieldEnum.PD.getText());
            logDO.setOldValue(String.join(",", oldPds.values()));
            logDO.setNewValue(String.join(",", newPds.values()));
            logs.add(logDO);
        }
        if (CollectionUtil.isNotEmpty(logs)) {
//            bizChangeLogMapper.batchInsert(logs);
        }
    }

    @Override
    public void addLogWhenStatusChange(Integer oldStatus, Integer newStatus, Long id, String action) {
        //{操作人}点击 {按钮名称} ,状态改为{操作后状态},
        if(!Objects.equals(oldStatus,newStatus)){
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
            logDO.setMainId(id);
            logDO.setField(BizChangeLogFieldEnum.PROJECT_STATUS.getText());
            logDO.setAction(action);
            logDO.setOldValue(ProjectStatusEnum.getTextByCode(oldStatus));
            logDO.setNewValue(ProjectStatusEnum.getTextByCode(newStatus));
//        bizChangeLogMapper.insert(logDO);
        }

    }

    @Override
    public void addLogWhenLinkOrUnlink(Map<Long, String> pdNameMap, String name, Long id,String linkOrUnlink) {
        List<BizChangeLogDO> logs = new ArrayList<>();
        pdNameMap.forEach((pId, pName) -> {
            //1.产品需求记录日志:{操作人}删除 {项目}:{项目A}
            BizChangeLogDO productLog = new BizChangeLogDO();
            productLog.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            productLog.setMainId(pId);
            productLog.setField(BizChangeLogTypeEnum.PROJECT.getText());
            productLog.setAction(linkOrUnlink);
            productLog.setOldValue(name);
            productLog.setNewValue(name);
            logs.add(productLog);
            //2.项目记录日志:{操作人}删除 {产品需求}:{产品需求A}
            BizChangeLogDO projectLog = new BizChangeLogDO();
            projectLog.setType(BizChangeLogTypeEnum.PROJECT.getCode());
            projectLog.setMainId(id);
            projectLog.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
            projectLog.setAction(linkOrUnlink);
            projectLog.setOldValue(pName);
            projectLog.setNewValue(pName);
            logs.add(projectLog);
        });
        if (CollectionUtil.isNotEmpty(logs)) {
//            bizChangeLogMapper.batchInsert(logs);
        }
    }
}
