package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.dto.BugOfflineBelongDistributionDTO;
import com.timevale.forward.dal.dto.BugOfflineCountDTO;
import com.timevale.forward.dal.dto.BugOfflineReasonDistributionDTO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOfflineListDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/02/24 09:35
 */
public interface BugOfflineMapper {
    /**
     * 插入
     *
     * @param bugOfflineDO 线下bugDO
     * @return 影响行数
     */
    int insert(BugOfflineDO bugOfflineDO);

    /**
     * 更新
     *
     * @param bugOfflineDO 线下bugDO
     * @return 影响行数
     */
    int update(BugOfflineDO bugOfflineDO);

    /**
     * 线下bug断开项目
     *
     * @param bugOfflineDOList 线下bug列表
     * @return 影响行数
     */
    int unlinkBugOffline(@Param("bugOfflineDOList") List<BugOfflineDO> bugOfflineDOList);

    /**
     * 选择id获取DO
     *
     * @param userIds 用户id
     * @return BugOfflineDO list
     */
    List<BugOfflineDO> selectByMembers(@Param("userIds") List<String> userIds);

    /**
     * 选择项目id获取DO
     *
     * @param projectId 项目id
     * @return BugOfflineDO list
     */
    List<BugOfflineDO> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 选择id获取DO
     *
     * @param id id
     * @return BugOfflineDO
     */
    BugOfflineDO get(@Param("id") Long id);

    List<BugOfflineDO> getByIds(@Param("ids") Collection<Long> ids, @Param("containDeleted") Boolean containDeleted);

    /**
     * 根据查询条件获取DO列表
     *
     * @param bugOfflineListCondition 线下bug查询列表条件
     * @return BugOfflineListDO List
     */
    List<BugOfflineListDO> selectByCondition(BugOfflineListCondition bugOfflineListCondition);

    /**
     * 根据bug的id删除线下bug
     *
     * @param bugOfflineId 参数
     * @return Boolean 返回值
     */
    Boolean deleteById(@Param("bugOfflineId") Long bugOfflineId);

    /**
     * 查询用户待修复线下BUG数量
     *
     * @param projectId 项目id
     * @return 项目下每个用户待修复线下bug数量
     */
    List<BugOfflineCountDTO> getBugCount(@Param("projectId") Long projectId);

    /**
     * 查询项目线下bug原因分布
     *
     * @param projectId 项目id
     * @return 线下bug分布列表
     */
    List<BugOfflineReasonDistributionDTO> getReasonDistribution(@Param("projectId") Long projectId);

    /**
     * 查询项目线下bug所属端分布
     *
     * @param projectId 项目id
     * @return 线下bug分布列表
     */
    List<BugOfflineBelongDistributionDTO> getBelongDistribution(@Param("projectId") Long projectId);

    /**
     * 根据线下bug的业务需求id查询线上bug
     *
     * @param bizDemandId 业务需求id
     * @return 返回值
     */
    BugOfflineDO selectByBizDemandId(@Param("bizDemandId") Long bizDemandId);

    /**
     * 通过产品线ID获取线下BUG
     *
     * @param productLineId 产品线ID
     * @return
     */
    @Select("select * from bug_offline where product_line_id=#{productLineId} AND is_deleted=false")
    List<BugOfflineDO> getByProductLineId(@Param("productLineId") Long productLineId);

    @Select("SELECT * FROM bug_offline WHERE operator_id=#{operatorId} AND is_deleted = false")
    List<BugOfflineDO> getByOperatorId(@Param("operatorId") String operatorId);

    @Select("SELECT * FROM bug_offline WHERE proposer_id=#{proposerId} AND is_deleted = false")
    List<BugOfflineDO> getByProposerId(@Param("proposerId") String proposerId);

    void updateOperator(@Param("ids") Collection<Long> ids, @Param("operator") String operator, @Param("operatorId") String operatorId);

    void updateProposer(@Param("ids") Collection<Long> ids, @Param("proposer") String proposer,  @Param("proposerId") String proposerId);

    List<BugOfflineDO> findBugsApproachingResolveTime(@Param("productLineIds") List<Long> productLineIds, @Param("timeFieldName") String timeFieldName, @Param("remindDays") int remindDays, @Param("remindTime") LocalTime remindTime);

    List<BugOfflineDO> findBugsOverdue(@Param("productLineIds") List<Long> productLineIds, @Param("timeFieldName") String timeFieldName);

    /**
     * 根据ID列表获取线下bug列表
     *
     * @param idList ID列表
     * @return 线下bug列表
     */
    List<BugOfflineDO> getByIdList(@Param("idList") List<Long> idList);

    /**
     * 批量更新线下bug的项目和产品线
     *
     * @param idList ID列表
     * @param projectId 项目ID
     * @param productLineId 产品线ID
     * @return 影响行数
     */
    int updateProjectAndProductLine(@Param("idList") List<Long> idList, @Param("projectId") Long projectId, @Param("productLineId") Long productLineId);
}
