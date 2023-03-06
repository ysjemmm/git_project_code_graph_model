package com.timevale.forward.service.copy;

import cn.hutool.core.bean.BeanUtil;
import com.timevale.forward.dal.condition.ProjectListChildCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.facade.api.query.ProductDemandLinkProjectQueryList;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.ProjectMD;
import com.timevale.forward.model.middle.ProjectSimpleMD;
import com.timevale.forward.service.utils.date.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Mapper(imports = {
        ProjectCategoryEnum.class,
        ProjectStageEnum.class,
        ProjectInnerTypeEnum.class,
        ProjectStatusEnum.class,
        ProjectLevelEnum.class,
        ProjectTypeEnum.class,
        PriorityEnum.class,
        BigDecimal.class,
        StringUtils.class,
        ProjectKindEnum.class,
        DateUtil.class
})
public interface ProjectCopier {

    ProjectCopier INSTANCE = Mappers.getMapper(ProjectCopier.class);

    /**
     * 转换转换DO
     *
     * @param projectAddReq 对象
     * @return ProductDemandDO
     */
    @Mapping(source = "pm.userName", target = "pm")
    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "sr.userName", target = "sr")
    @Mapping(source = "sr.userId", target = "srId")
    @Mapping(source = "principal.userName", target = "principal")
    @Mapping(source = "principal.userId", target = "principalId")
    @Mapping(source = "otnPrincipal.userName", target = "otnPrincipal")
    @Mapping(source = "otnPrincipal.userId", target = "otnPrincipalId")
    @Mapping(target = "status", expression = "java(ProjectStatusEnum.WAITING.getCode())")
    ProjectDO convert(ProjectAddReq projectAddReq);

    @Mapping(target = "status", source = "req.status")
    @Mapping(target = "parentIds", expression = "java(project.getParentList())")
    @Mapping(target = "parentIdsRegexp", expression = "java(\"^\" + project.getParentIds() + \".\")")
    ProjectListChildCondition convert(ProjectChildListReq req, ProjectDO project);

    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "pm.userName", target = "pm")
    @Mapping(target = "validStages", expression = "java(ProjectStageEnum.getAllStageJson(ProjectCategoryEnum.INNER_PROJECT))")
    @Mapping(target = "category", expression = "java(ProjectCategoryEnum.INNER_PROJECT.getCode())")
    ProjectDO convert(ProjectInnerAddReq projectInnerAddReq);

    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "pm.userName", target = "pm")
    @Mapping(target = "expectedIncome", ignore = true)
    ProjectDO sreq2do(ProjectSimpleModifyReq req);

    /**
     * 转换转换DO
     *
     * @param projectModifyReq 对象
     * @return ProjectDO
     */
    @Mapping(source = "pds", target = "pds", qualifiedByName = "mapping")
    @Mapping(source = "pm.userName", target = "pm")
    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "sr.userName", target = "sr")
    @Mapping(source = "sr.userId", target = "srId")
    @Mapping(source = "principal.userName", target = "principal")
    @Mapping(source = "principal.userId", target = "principalId")
    @Mapping(source = "otnPrincipal.userName", target = "otnPrincipal")
    @Mapping(source = "otnPrincipal.userId", target = "otnPrincipalId")
    ProjectDO convert(ProjectModifyReq projectModifyReq);
    
    /**
     * 转换转换DO
     *
     * @param projectDO 对象
     * @return ProjectDetailVO
     */
    @Mapping(source = "pm", target = "pmName")
    @Mapping(target = "kindName", expression = "java(ProjectKindEnum.getTextByCode(projectDO.getKind()))")
    @Mapping(target = "typeName", expression = "java(ProjectTypeEnum.getTextByCode(projectDO.getType()))")
    @Mapping(target = "levelName", expression = "java(ProjectLevelEnum.getTextByCode(projectDO.getLevel()))")
    @Mapping(target = "priorityName", expression = "java(PriorityEnum.getTextByCode(projectDO.getPriority()))")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(projectDO.getStatus()))")
    ProjectDetailVO convert(ProjectDO projectDO);

    /**
     * 转换转换DO
     *
     * @param query 对象
     * @return ProjectListCondition
     */

    @Mapping(target = "createDateLeft", expression = "java(DateUtil.getStartOfDay(query.getCreateDateLeft()))")
    @Mapping(target = "createDateRight", expression = "java(DateUtil.getEndOfDay(query.getCreateDateRight()))")
    @Mapping(target = "planEndDateLeft", expression = "java(DateUtil.getStartOfDay(query.getPlanEndDateLeft()))")
    @Mapping(target = "planEndDateRight", expression = "java(DateUtil.getEndOfDay(query.getPlanEndDateRight()))")
    @Mapping(target = "planStartDateLeft", expression = "java(DateUtil.getStartOfDay(query.getPlanStartDateLeft()))")
    @Mapping(target = "planStartDateRight", expression = "java(DateUtil.getEndOfDay(query.getPlanStartDateRight()))")
    @Mapping(target = "actualEndDateLeft", expression = "java(DateUtil.getStartOfDay(query.getActualEndDateLeft()))")
    @Mapping(target = "actualEndDateRight", expression = "java(DateUtil.getEndOfDay(query.getActualEndDateRight()))")
    @Mapping(target = "actualTestDateLeft", expression = "java(DateUtil.getStartOfDay(query.getActualTestDateLeft()))")
    @Mapping(target = "actualTestDateRight", expression = "java(DateUtil.getEndOfDay(query.getActualTestDateRight()))")
    @Mapping(target = "actualStartDateLeft", expression = "java(DateUtil.getStartOfDay(query.getActualStartDateLeft()))")
    @Mapping(target = "actualStartDateRight", expression = "java(DateUtil.getEndOfDay(query.getActualStartDateRight()))")
    ProjectListCondition convert(ProjectQueryList query);

    /**
     * 转换转换DO
     *
     * @param projectListDO 对象
     * @return ProjectDetailVO
     */
    @Mapping(target = "pmName", source = "pm")
    @Mapping(target = "kindName", expression = "java(ProjectKindEnum.getTextByCode(projectListDO.getKind()))")
    @Mapping(target = "typeName", expression = "java(ProjectTypeEnum.getTextByCode(projectListDO.getType()))")
    @Mapping(target = "levelName", expression = "java(ProjectLevelEnum.getTextByCode(projectListDO.getLevel()))")
    @Mapping(target = "priorityName", expression = "java(PriorityEnum.getTextByCode(projectListDO.getPriority()))")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(projectListDO.getStatus()))")
    @Mapping(target = "nodeDepth", expression = "java(StringUtils.split(projectListDO.getParentIds(), ',').length)")
    @Mapping(target = "innerTypeName", expression = "java(ProjectInnerTypeEnum.getTextByCode(projectListDO.getInnerType()))")
    ProjectVO convert(ProjectListDO projectListDO);

    /**
     * 转换转换DO
     *
     * @param projectListDO 对象
     * @return ProjectVO
     */
    List<ProjectVO> convert(List<ProjectListDO> projectListDO);

    /**
     * 转换转换DO
     *
     * @param projectQueryList 对象
     * @return ProjectListCondition
     */
    ProjectListCondition convert(ProductDemandLinkProjectQueryList projectQueryList);

    /**
     * 转换
     *
     * @param projectDO 项目DO
     * @return ProjectBaseVO
     */
    @Mapping(source = "id", target = "projectId")
    @Mapping(source = "name", target = "projectName")
    ProjectBaseVO convertTo(ProjectDO projectDO);

    /**
     * 转换转换DO
     *
     * @param projectDO 对象
     * @return ProjectVO
     */
    @Mapping(source = "pm", target = "pmName")
    ProjectVO transform(ProjectDO projectDO);


    /**
     * 转换转换DO
     *
     * @param projectDO 对象
     * @return ProjectDO
     */
    @Mapping(target = "pmName", source = "pm")
    ProjectMD change(ProjectDO projectDO);

    ProjectSimpleMD do2smd(ProjectDO projectDO);

    /**
     *
     * @param projectDateModifyReq projectDateModifyReq
     * @return ProjectDO
     */
    ProjectDO convert(ProjectDateModifyReq projectDateModifyReq);

    @Mapping(target = "pmName", source = "pm")
    @Mapping(target = "levelName", expression = "java(ProjectLevelEnum.getTextByCode(projectDO.getLevel()))")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(projectDO.getStatus()))")
    @Mapping(target = "innerTypeName", expression = "java(ProjectInnerTypeEnum.getTextByCode(projectDO.getInnerType()))")
    ProjectInnerDetailVO do2Vo(ProjectDO projectDO);


    @Named("mapping")
    default List<PersonDO> change(List<PersonAddReq> list){
        return BeanUtil.copyToList(list, PersonDO.class);
    }

    @Mapping(target = "children", ignore = true)
    @Mapping(target = "projectId", source = "id")
    @Mapping(target = "projectName", source = "name")
    ProjectTreeVO convertTree(ProjectDO project);
    List<ProjectTreeVO> convertTree(List<ProjectDO> projects);
}
