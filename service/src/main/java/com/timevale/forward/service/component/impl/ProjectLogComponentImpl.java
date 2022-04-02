package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.middle.ProjectMD;
import com.timevale.forward.service.component.BizChangeLogComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProjectLogComponentImpl implements BizChangeLogComponent<ProjectDO> {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private PersonComponent personComponent;

    @Override
    public void addLogWhenModifyData(ProjectDO oldObj, ProjectDO newObj) {
        ProjectMD oldProject = ProjectCopier.INSTANCE.change(oldObj);
        ProjectMD newProject = ProjectCopier.INSTANCE.change(newObj);
        List<BizChangeLogDO> logs = FieldCompareUtil.commonCompare(oldProject, newProject, BizChangeLogDO.class);
        if(!CollectionUtil.isEqualList(oldObj.getProductLineIds(),newObj.getProductLineIds())){
            List<Long>productLineIds= new ArrayList<>(oldObj.getProductLineIds());
            productLineIds.addAll(newObj.getProductLineIds());

            Map<Long, String> productLineMap = productLineMapper.selectByIds(productLineIds)
                    .stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName));
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
            logDO.setMainId(oldObj.getId());
            logDO.setField(BizChangeLogFieldEnum.PRODUCT_LINE.getText());

            List<String>oldValue=new ArrayList<>();
            oldObj.getProductLineIds().forEach(a->{
                oldValue.add(productLineMap.get(a));
            });
            List<String>newValue=new ArrayList<>();
            newObj.getProductLineIds().forEach(a->{
                newValue.add(productLineMap.get(a));
            });
            logDO.setOldValue(String.join(",",oldValue));
            logDO.setNewValue(String.join(",",newValue));
            logs.add(logDO);
        }


        Map<String, String> oldPds = personComponent.select(oldObj.getId(), PersonTypeEnum.PROJECT_PD.getCode())
                .stream().collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName));
        Map<String, String> newPds = newObj.getPds().stream().collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName));
        if(!CollectionUtil.isEqualList(oldPds.keySet(),newPds.keySet())){
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
            logDO.setMainId(oldObj.getId());
            logDO.setField(BizChangeLogFieldEnum.PD.getText());
            logDO.setOldValue(String.join(",", oldPds.values()));
            logDO.setNewValue(String.join(",", newPds.values()));
            logs.add(logDO);
        }
        if(CollectionUtil.isNotEmpty(logs)){
            bizChangeLogMapper.batchInsert(logs);
        }
    }
}
