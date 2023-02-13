package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.ProjectMD;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
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

    @Resource
    private ProjectProductLineMapper projectProductLineMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private BizDemandLogComponent bizDemandLogComponent;

    @Resource
    private ProductCustomDemandMapper productCustomDemandMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private CustomDemandMapper customDemandMapper;


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
        List<Long> oldProductLineIds = projectProductLineMapper.get(oldObj.getId())
                .stream().map(ProjectProductLineDO::getProductLineId).collect(Collectors.toList());
        if (!CollectionUtil.isEqualList(oldProductLineIds, newObj.getProductLineIds())) {
            List<Long> productLineIds = new ArrayList<>(oldProductLineIds);
            productLineIds.addAll(newObj.getProductLineIds());

            Map<Long, String> productLineMap = productLineMapper.selectByIds(productLineIds)
                    .stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (v1, v2) -> v2));

            String oldValue = oldProductLineIds.stream().map(productLineMap::get).collect(Collectors.joining(","));
            String newValue = newObj.getProductLineIds().stream().map(productLineMap::get).collect(Collectors.joining(","));
            logs.add(createLog(oldObj.getId(), BizChangeLogFieldEnum.PRODUCT_LINE.getText(), oldValue, newValue, null));
        }

        //产品经理
        Map<String, String> oldPds = personComponent.select(oldObj.getId(), PersonTypeEnum.PROJECT_PD.getCode())
                .stream().collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName, (v1, v2) -> v2));
        Map<String, String> newPds = newObj.getPds().stream().collect(Collectors.toMap(PersonDO::getUserId, PersonDO::getUserName, (v1, v2) -> v2));
        if (!CollectionUtil.isEqualList(oldPds.keySet(), newPds.keySet())) {
            String oldValue = String.join(",", oldPds.values());
            String newValue = String.join(",", newPds.values());
            logs.add(createLog(oldObj.getId(), BizChangeLogFieldEnum.PD.getText(), oldValue, newValue, null));
        }
        //
        if (!Objects.equals(oldObj.getPlanEndDate(), newObj.getPlanEndDate()) || newObj.getActualEndDate() != null) {
            //计划或实际时间变动可能影响到需求的项目发布时间
            List<Long> productDemandIds = projectProductDemandMapper.getByProjectId(oldObj.getId())
                    .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(productDemandIds)) {
                List<Long> bizDemandIds = productBizDemandMapper.selectByProductDemandIds(productDemandIds)
                        .stream().map(ProductBizDemandDO::getBizDemandId).distinct().collect(Collectors.toList());
                bizDemandIds.forEach(bid -> {
                    Date publishDate = bizDemandMapper.selectById(bid).getProjectEndDate();
                    log.info("bid={},planEndDate={},publishDate={}", bid, newObj.getPlanEndDate(), publishDate);
                    BizChangeLogDO bizChangeLogDO = bizChangeLogMapper.getProjectPublishDate(bid, BizChangeLogTypeEnum.BIZ_DEMAND.getCode(), BizChangeLogFieldEnum.PROJECT_RELEASE_DATE.getText());
                    //发布时间已变为当前需要更新的时间
                    String oldValue = bizChangeLogDO == null ? "" : bizChangeLogDO.getNewValue();
                    if (Objects.equals(newObj.getActualEndDate(), publishDate)) {
                        String newValue = DateUtil.parseToString(newObj.getActualEndDate(), DateStyle.YYYY_MM_DD);
                        logs.add(bizDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, bid));
                    } else if (Objects.equals(newObj.getPlanEndDate(), publishDate)) {
                        String newValue = DateUtil.parseToString(newObj.getPlanEndDate(), DateStyle.YYYY_MM_DD);
                        logs.add(bizDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, bid));
                    }
                });

                List<Long> customDemandIds = productCustomDemandMapper.selectByProductDemandIds(productDemandIds)
                        .stream().map(ProductCustomDemandDO::getCustomDemandId).distinct().collect(Collectors.toList());
                customDemandIds.forEach(cid -> {
                    Date publishDate = customDemandMapper.selectById(cid).getProjectEndDate();
                    log.info("cid={},planEndDate={},publishDate={}", cid, newObj.getPlanEndDate(), publishDate);
                    BizChangeLogDO bizChangeLogDO = bizChangeLogMapper.getProjectPublishDate(cid, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode(), BizChangeLogFieldEnum.PROJECT_RELEASE_DATE.getText());
                    String oldValue = bizChangeLogDO == null ? "" : bizChangeLogDO.getNewValue();
                    if (Objects.equals(newObj.getActualEndDate(), publishDate)) {
                        String newValue = DateUtil.parseToString(newObj.getActualEndDate(), DateStyle.YYYY_MM_DD);
                        logs.add(bizDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, cid, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode()));
                    } else if (Objects.equals(newObj.getPlanEndDate(), publishDate)) {
                        String newValue = DateUtil.parseToString(newObj.getPlanEndDate(), DateStyle.YYYY_MM_DD);
                        logs.add(bizDemandLogComponent.buildLogWhenPublishDateChange(oldValue, newValue, cid, BizChangeLogTypeEnum.CUSTOM_DEMAND.getCode()));
                    }
                });
            }
        }

        logs.forEach(a -> {
            a.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            a.setCreateManId(userInfo.getId());
        });
        if (CollectionUtil.isNotEmpty(logs)) {
            bizChangeLogMapper.batchInsert(logs);
        }
    }

    @Override
    public void addLogWhenSimpleModifyData(ProjectDO oldObj, ProjectDO newObj) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        //{操作人} 把{字段名称} 从{原内容}改为{最新内容}
        ProjectMD oldProject = ProjectCopier.INSTANCE.change(oldObj);
        ProjectMD newProject = ProjectCopier.INSTANCE.change(newObj);
        List<BizChangeLogDO> logs = FieldCompareUtil.commonCompare(oldProject, newProject, BizChangeLogDO.class);
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
        //1){操作人}点击 {按钮名称} ,状态改为{操作后状态}
        String oldValue = ProjectStatusEnum.getTextByCode(oldStatus);
        String newValue = ProjectStatusEnum.getTextByCode(newStatus);
        BizChangeLogDO logDO = createLog(id, BizChangeLogFieldEnum.PROJECT_STATUS.getText(), oldValue, newValue, action);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String createMan = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        String createManId = userInfo.getId();
        logDO.setCreateMan(createMan);
        logDO.setCreateManId(createManId);
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

    /**
     * @param oldValue oldValue
     * @param newValue newValue
     * @param id       id
     * @param field    field
     */
    @Override
    public void addLogWhenContentChange(String oldValue, String newValue, Long id, String field) {
        //1){操作人}把{字段}由{旧值}改为{新值}
        if (!Objects.equals(oldValue, newValue)) {
            BizChangeLogDO logDO = createLog(id, field, oldValue, newValue, null);
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            String createMan = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
            String createManId = userInfo.getId();
            logDO.setCreateMan(createMan);
            logDO.setCreateManId(createManId);
            bizChangeLogMapper.insert(logDO);
        }
    }

    @Override
    public void addLogWhenContentChange(String oldValue, String newValue, Long id, String field, String action) {
        //1){操作人}把{字段}由{旧值}改为{新值}
        if (!Objects.equals(oldValue, newValue)) {
            BizChangeLogDO logDO = createLog(id, field, oldValue, newValue, action);
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            String createMan = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
            String createManId = userInfo.getId();
            logDO.setCreateMan(createMan);
            logDO.setCreateManId(createManId);
            bizChangeLogMapper.insert(logDO);
        }
    }

    @Override
    public void addAppendChildLog(Long id, String childName) {
        BizChangeLogDO log = new BizChangeLogDO();
        log.setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setMainId(id)
                .setField(StringUtils.EMPTY)
                .setAction(ButtonActionEnum.APPEND_CHILD.getText())
                .setNewValue(childName);
        bizChangeLogMapper.insert(log);
    }

    @Override
    public void addAttachParentLog(Long id, String parentName) {
        BizChangeLogDO log = new BizChangeLogDO();
        log.setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setMainId(id)
                .setField(StringUtils.EMPTY)
                .setAction(ButtonActionEnum.LINK_PARENT.getText())
                .setNewValue(parentName);
        bizChangeLogMapper.insert(log);
    }

    @Override
    public void addDeleteChildLog(Long id, String childName) {
        BizChangeLogDO log = new BizChangeLogDO();
        log.setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setMainId(id)
                .setField(StringUtils.EMPTY)
                .setAction(ButtonActionEnum.DELETE_CHILD.getText())
                .setNewValue(childName);
        bizChangeLogMapper.insert(log);
    }

    @Override
    public void addDetachParentLog(Long id, String parentName) {
        BizChangeLogDO log = new BizChangeLogDO();
        log.setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setMainId(id)
                .setField(StringUtils.EMPTY)
                .setAction(ButtonActionEnum.UNLINK_PARENT.getText())
                .setNewValue(parentName);
        bizChangeLogMapper.insert(log);
    }

    private BizChangeLogDO createLog(Long mainId, String field, String oldValue, String newValue, String action) {
        BizChangeLogDO logDO = new BizChangeLogDO();
        logDO.setType(BizChangeLogTypeEnum.PROJECT.getCode());
        logDO.setMainId(mainId);
        logDO.setField(field);
        logDO.setOldValue(oldValue);
        logDO.setNewValue(newValue);
        logDO.setAction(action);
        return logDO;
    }
}
