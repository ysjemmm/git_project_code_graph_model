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
import com.timevale.forward.facade.api.result.ProjectBaseVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import com.timevale.forward.facade.api.result.ProjectInnerDetailVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.ProjectMD;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(imports = {
        ProjectCategoryEnum.class,
        ProjectValidStages.class,
        ProjectStatusEnum.class,
        ProjectLevelEnum.class,
        ProjectTypeEnum.class,
        PriorityEnum.class,
})
public interface ProjectCopier {

    ProjectCopier INSTANCE = Mappers.getMapper(ProjectCopier.class);

    /**
     * 转换转换DO
     *
     * @param projectAddReq 对象
     * @return ProductDemandDO
     */
    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "pm.userName", target = "pm")
    ProjectDO convert(ProjectAddReq projectAddReq);

    @Mapping(target = "status", source = "req.status")
    @Mapping(target = "parentIds", expression = "java(project.getParentList())")
    @Mapping(target = "parentIdsRegexp", expression = "java(\"^\" + project.getParentIds() + \".\")")
    ProjectListChildCondition convert(ProjectChildListReq req, ProjectDO project);

    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "pm.userName", target = "pm")
    @Mapping(target = "validStages", expression = "java(ProjectValidStages.getAllStageJson())")
    @Mapping(target = "category", expression = "java(ProjectCategoryEnum.INNER_PROJECT.getCode())")
    ProjectDO convert(ProjectInnerAddReq projectInnerAddReq);

    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "pm.userName", target = "pm")
    ProjectDO convert(ProjectSimpleModifyReq projectSimpleModifyReq);

    /**
     * 转换转换DO
     *
     * @param projectModifyReq 对象
     * @return ProjectDO
     */
    @Mapping(source = "pds", target = "pds", qualifiedByName = "mapping")
    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "pm.userName", target = "pm")
    ProjectDO convert(ProjectModifyReq projectModifyReq);
    
    /**
     * 转换转换DO
     *
     * @param projectDO 对象
     * @return ProjectDetailVO
     */
    @Mapping(source = "pm", target = "pmName")
    ProjectDetailVO convert(ProjectDO projectDO);

    /**
     * 转换转换DO
     *
     * @param projectQueryList 对象
     * @return ProjectListCondition
     */
    ProjectListCondition convert(ProjectQueryList projectQueryList);

    /**
     * 转换转换DO
     *
     * @param projectListDO 对象
     * @return ProjectDetailVO
     */
    @Mapping(target = "pmName", source = "pm")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(projectListDO.getStatus()))")
    @Mapping(target = "typeName", expression = "java(ProjectTypeEnum.getTextByCode(projectListDO.getType()))")
    @Mapping(target = "priorityName", expression = "java(PriorityEnum.getTextByCode(projectListDO.getPriority()))")
    @Mapping(target = "levelName", expression = "java(ProjectLevelEnum.getTextByCode(projectListDO.getLevel()))")
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

    /**
     *
     * @param projectDateModifyReq projectDateModifyReq
     * @return ProjectDO
     */
    ProjectDO convert(ProjectDateModifyReq projectDateModifyReq);

    @Mapping(target = "pmName", source = "pm")
    @Mapping(target = "statusName", expression = "java(ProjectStatusEnum.getTextByCode(projectDO.getStatus()))")
    ProjectInnerDetailVO do2Vo(ProjectDO projectDO);


    @Named("mapping")
    default List<PersonDO> change(List<PersonAddReq> list){
        return BeanUtil.copyToList(list, PersonDO.class);
    }

}
