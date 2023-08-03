package com.timevale.forward.service.component;

import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.model.dto.ModifyProjectCheckDTO;
import com.timevale.forward.model.dto.ModifyProjectProcessedBundle;
import com.timevale.mandarin.common.query.QueryBase;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface ProjectComponent {
    /**
     *
     * @param projectListCondition 查询条件
     * @return 列表
     */
    QueryResultVO<ProjectVO> page(ProjectListCondition projectListCondition, List<Long> projectIds);

    /**
     * 填充项目信息
     * @param projectNodes projectNodes
     * @param projectDO projectDO
     */
    void fillInfo(List<ProjectNodeDO> projectNodes, ProjectDO projectDO);

    /**
     * 根据项目节点，获取项目状态
     *
     * @param projectId 项目id
     */
    Integer getStatus(Long projectId);

    /**
     * 更新项目节点状态
     *
     * @param projectId 项目id
     */
    void updateNodeStatus(Long projectId);

    /**
     *
     * @param projectId projectId
     * @return Long
     */
    List<Long> getLinkProductDemandIds(Long projectId);

    /**
     * 获得所有进行中的项目id
     */
    List<ProjectDO> pageAllOngoingProjects(QueryBase queryBase);

    /**
     * 添加子项目节点
     */
    void attachChildProject(ProjectDO parent, ProjectDO child);

    /**
     * 删除子项目节点
     */
    void deleteChildProject(ProjectDO parent, ProjectDO child);

    /**
     * 获取url
     *
     * @param projectId 项目id
     * @return {@link String}
     */
    String getUrl(Long projectId);

    ProjectDO getByBizDemandId(Long bizDemandId);

    /**
     * 更新pbu
     *
     * @param projectId 项目id
     * @param pbuIds    试译id
     */
    void updatePbu(Long projectId, Collection<Long>pbuIds);

    /**
     * 更新业务域
     *
     * @param projectId    项目id
     * @param bizDomainIds 业务域id
     */
    void updateBizDomain(Long projectId, Collection<Long>bizDomainIds);

    
    /**
     * 检查项目修改
     * @param projectModifyReq 项目修改请求
     * @param dataHandler 数据处理器
     * @return {@link ModifyProjectProcessedBundle}
     */
    ModifyProjectProcessedBundle checkProjectModify(ProjectModifyReq projectModifyReq,
                                                    Consumer<ModifyProjectCheckDTO> dataHandler);

}
