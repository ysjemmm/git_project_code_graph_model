package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.AutomationRuleDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自动化规则Mapper
 *
 * @author by qiyuan
 * @date 2025/10/09 16:38
 */
public interface AutomationRuleMapper {

    /**
     * 根据主键id获取自动化规则
     *
     * @param id 主键id
     * @return 自动化规则DO
     */
    AutomationRuleDO get(@Param("id") Long id);

    /**
     * 根据项目id和业务类型查询自动化规则列表
     *
     * @param projectId 项目id
     * @param bizType   业务类型
     * @return 自动化规则列表
     */
    List<AutomationRuleDO> listByProjectIdAndBizType(@Param("projectId") Long projectId, @Param("bizType") String bizType);

    /**
     * 根据项目id和业务类型查询自动化规则列表
     *
     * @param triggerType 触发类型
     * @param isEnabled  是否启用
     * @return 自动化规则列表
     */
    List<AutomationRuleDO> findByTriggerTypeAndIsEnabled(@Param("triggerType") String triggerType, @Param("isEnabled") Boolean isEnabled);

    /**
     * 新增一条自动化规则
     *
     * @param automationRuleDO 自动化规则DO
     * @return 影响行数
     */
    int insert(AutomationRuleDO automationRuleDO);

    /**
     * 更新自动化规则
     *
     * @param automationRuleDO 自动化规则DO
     * @return 影响行数
     */
    int update(AutomationRuleDO automationRuleDO);

    /**
     * 删除自动化规则
     *
     * @param id          主键id
     * @param modifyManId 修改人id
     * @param modifyMan   修改人
     * @return 影响行数
     */
    int delete(@Param("id") Long id, @Param("modifyManId") String modifyManId, @Param("modifyMan") String modifyMan);
}
