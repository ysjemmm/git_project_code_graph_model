package com.timevale.forward.service.copy;

import cn.hutool.core.bean.BeanUtil;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.facade.api.query.ProductDemandLinkProjectQueryList;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.result.ProjectBaseVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.middle.ProjectMD;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProjectCopier {

    ProjectCopier INSTANCE = Mappers.getMapper(ProjectCopier.class);

    /**
     * 转换转换DO
     *
     * @param projectAddReq 对象
     * @return ProductDemandDO
     */
    ProjectDO convert(ProjectAddReq projectAddReq);
    /**
     * 转换转换DO
     *
     * @param projectModifyReq 对象
     * @return ProjectDO
     */
    @Mapping(source = "pds", target = "pds", qualifiedByName = "mapping")
    @Mapping(source = "pm.userId", target = "pmId")
    @Mapping(source = "pm.userName", target = "pmName")
    ProjectDO convert(ProjectModifyReq projectModifyReq);
    
    /**
     * 转换转换DO
     *
     * @param projectDO 对象
     * @return ProjectDetailVO
     */
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
    ProjectVO transform(ProjectDO projectDO);


    /**
     * 转换转换DO
     *
     * @param projectDO 对象
     * @return ProjectDO
     */
    ProjectMD change(ProjectDO projectDO);


    @Named("mapping")
    default List<PersonDO> change(List<PersonAddReq> list){
        return BeanUtil.copyToList(list, PersonDO.class);
    }

}
