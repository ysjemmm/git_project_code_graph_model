package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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

//        ProjectMD oldProject = ProjectCopier.INSTANCE.change(oldObj);
//        ProjectMD newProject = ProjectCopier.INSTANCE.change(newObj);
//        List<BizChangeLogDO> logs = FieldCompareUtil.commonCompare(oldProject, newProject, BizChangeLogDO.class);
//        if(!CollectionUtil.isEqualList(oldObj.getProductLineIds(),newObj.getProductLineIds())){
//            List<Long> productLineIds= new ArrayList<>(oldObj.getProductLineIds());
//            productLineIds.addAll(newObj.getProductLineIds());
//
//            Map<Long, String> productLineMap = productLineMapper.selectByIds(productLineIds)
//                    .stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName));
//            BizChangeLogDO logDO = new BizChangeLogDO();
//            logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
//            logDO.setMainId(oldObj.getId());
//            logDO.setField(BizChangeLogFieldEnum.PRODUCT_LINE.getText());
//            String oldValue = oldObj.getProductLineIds().stream().map(productLineMap::get).collect(Collectors.joining(","));
//            String newValue = newObj.getProductLineIds().stream().map(productLineMap::get).collect(Collectors.joining(","));
//            logDO.setOldValue(oldValue);
//            logDO.setNewValue(newValue);
//            logs.add(logDO);
//        }
//
//
//        Map<String, String> oldPds = personComponent.select(oldObj.getId(), PersonTypeEnum.PROJECT_PD.getCode())
//                .stream().collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName));
//        Map<String, String> newPds = newObj.getPds().stream().collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName));
//        if(!CollectionUtil.isEqualList(oldPds.keySet(),newPds.keySet())){
//            BizChangeLogDO logDO = new BizChangeLogDO();
//            logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
//            logDO.setMainId(oldObj.getId());
//            logDO.setField(BizChangeLogFieldEnum.PD.getText());
//            logDO.setOldValue(String.join(",", oldPds.values()));
//            logDO.setNewValue(String.join(",", newPds.values()));
//            logs.add(logDO);
//        }
//        if(CollectionUtil.isNotEmpty(logs)){
//            bizChangeLogMapper.batchInsert(logs);
//        }
    }
}
