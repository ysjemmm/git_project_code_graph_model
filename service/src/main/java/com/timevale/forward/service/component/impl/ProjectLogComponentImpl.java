package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.ProjectMD;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class ProjectLogComponentImpl implements ProjectLogComponent {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private PersonComponent personComponent;

    /**
     * 编辑时,记录日志
     *
     * @param oldObj oldObj
     * @param newObj newObj
     */
    @Override
    public void addLogWhenModifyData(ProjectDO oldObj, ProjectDO newObj) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        //{操作人} 把{字段名称} 从{原内容}改为{最新内容}
        ProjectMD oldProject = ProjectCopier.INSTANCE.change(oldObj);
        ProjectMD newProject = ProjectCopier.INSTANCE.change(newObj);
        List<BizChangeLogDO> logs = FieldCompareUtil.commonCompare(oldProject, newProject, BizChangeLogDO.class);

        //产品线
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

        //产品经理
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

        logs.forEach(a -> {
            a.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            a.setCreateManId(userInfo.getId());
        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }

    /**
     * 按钮点击时引起的项目状态变化
     *
     * @param oldStatus oldStatus
     * @param newStatus newStatus
     * @param id        id
     * @param action    action
     */
    @Override
    public void addLogWhenStatusChange(Integer oldStatus, Integer newStatus, Long id, String action) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        //1){操作人}点击 {按钮名称} ,状态改为{操作后状态},2)编辑项目时:把{项目状态}从{原状态} 改为{新状态}
        BizChangeLogDO logDO = new BizChangeLogDO();
        logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
        logDO.setMainId(id);
        logDO.setField(BizChangeLogFieldEnum.PROJECT_STATUS.getText());
        logDO.setAction(action);
        logDO.setOldValue(ProjectStatusEnum.getTextByCode(oldStatus));
        logDO.setNewValue(ProjectStatusEnum.getTextByCode(newStatus));
        logDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        logDO.setCreateManId(userInfo.getId());
        bizChangeLogMapper.insert(logDO);

    }

    /**
     * 关联/删除关联/作废项目时,双向记录日志
     *
     * @param name         name
     * @param id           id
     * @param pdNameMap    pdNameMap
     * @param linkOrUnlink linkOrUnlink
     */
    @Override
    public void addLogWhenLinkOrUnlink(String name, Long id, Map<Long, String> pdNameMap, String linkOrUnlink) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        //为空表示非主动点击删除按钮,赋值SYSTEM-SYSTEM
        boolean empty = StringUtils.isEmpty(linkOrUnlink);
        String action = empty ? ButtonActionEnum.UN_LINK.getText() : linkOrUnlink;
        String createMan = empty ? CommonConstant.SYSTEM : userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        String createManId = empty ? CommonConstant.SYSTEM : userInfo.getId();

        List<BizChangeLogDO> logs = new ArrayList<>();
        pdNameMap.forEach((pId, pName) -> {
            //1.项目记录日志:{操作人}添加/删除 {产品需求}:{产品需求A}
            BizChangeLogDO projectLog = new BizChangeLogDO();
            projectLog.setType(BizChangeLogTypeEnum.PROJECT.getCode());
            projectLog.setMainId(id);
            projectLog.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
            projectLog.setAction(action);
            projectLog.setOldValue(pName);
            projectLog.setNewValue(pName);
            projectLog.setCreateMan(createMan);
            projectLog.setCreateManId(createManId);
            logs.add(projectLog);

            //2.产品需求记录日志:{操作人}添加/删除  {项目}:{项目A}
            BizChangeLogDO pdLog = new BizChangeLogDO();
            pdLog.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            pdLog.setMainId(pId);
            pdLog.setField(BizChangeLogTypeEnum.PROJECT.getText());
            pdLog.setAction(action);
            pdLog.setOldValue(name);
            pdLog.setNewValue(name);
            pdLog.setCreateMan(createMan);
            pdLog.setCreateManId(createManId);
            logs.add(pdLog);
        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }
}
