package com.timevale.forward.dal.dao;

import com.github.pagehelper.Page;
import com.timevale.forward.dal.condition.ProjectListChildCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface ProjectMapper {
    /**
     * 新增单条项目
     *
     * @param projectDO 项目
     * @return int
     */
    int insert(ProjectDO projectDO);

    /**
     * 内部项目-新增
     *
     * @param projectDO 项目
     * @return int
     */
    int innerInsert(ProjectDO projectDO);

    /**
     * 查询
     *
     * @param id id
     * @return 项目信息
     */
    ProjectDO get(@Param("id") Long id);

    /**
     * 得到项目所有id
     */
    List<Long> getAllId();

    /**
     * 新增单条项目
     *
     * @param projectDO 项目
     * @return int
     */
    int update(ProjectDO projectDO);

    /**
     * 完成更新（可以为null）
     *
     * @param projectDO 项目
     */
    void fullUpdateById(ProjectDO projectDO);

    /**
     * 查询
     *
     * @param productDemandId 产品需求id
     * @return 项目信息
     */
    ProjectDO getByProductDemandId(@Param("productDemandId") Long productDemandId);


    /**
     * 选择通过产品需求id列表
     *
     * @param productDemandIdList 产品需求id列表
     * @return ProjectDO
     */
    List<ProjectDO> selectByProductDemandIdList(@Param("productDemandIdList") List<Long> productDemandIdList);

    /**
     * 通过团队成员选择
     *
     * @param teamMemberList 团队成员列表
     * @return ProjectDO List
     */
    List<ProjectDO> selectByTeamMember(@Param("teamMemberList") List<String> teamMemberList);

    /**
     * @param projectIds     项目id
     * @param productLineIds 产品线id
     */
    List<Long> getProjectIds(@Param("projectIds") List<Long> projectIds, @Param("productLineIds") List<Long> productLineIds, @Param("bizDomainIds") List<Long> bizDomainIds);


    /**
     * 根据查询条件获取项目列表
     *
     * @param condition 查询条件
     * @return ProjectDO列表
     */
    List<ProjectListDO> list(ProjectListCondition condition);

    /**
     * 查询
     *
     * @param name name
     * @return 产品需求DO
     */
    ProjectDO getByName(@Param("name") String name);

    /**
     * 查询
     *
     * @param ids id
     * @return 项目信息
     */
    List<ProjectDO> getByIds(@Param("ids") Collection<Long> ids);

    /**
     * 查询id
     *
     * @param productLineId 产品线id
     * @param userId        用户id
     * @return 项目DOList
     */
    List<ProjectDO> selectByProductLine(@Param("productLineId") Long productLineId, @Param("userId") String userId);

    /**
     * 查询
     *
     * @param status status
     * @return 项目信息
     */
    List<ProjectDO> getByStatus(@Param("status") List<Integer> status);

    /**
     * 状态重算时使用
     *
     * @param projectDO 项目
     * @return int
     */
    int updateStatus(ProjectDO projectDO);

    /**
     * 节点状态计算-不修改更新时间
     *
     * @param projectDO 项目DO
     */
    int updateNodeStatus(ProjectDO projectDO);

    /**
     * 获得所有进行中的项目id
     */
    List<ProjectDO> pageAllOngoingProjects();

    /**
     * 查询
     *
     * @param name name
     * @return 产品需求DO
     */
    List<ProjectDO> getByLikeName(@Param("name") String name);

    Page<ProjectListDO> listChildren(ProjectListChildCondition condition);
}
