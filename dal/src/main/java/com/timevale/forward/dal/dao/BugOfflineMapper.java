package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.dto.BugOfflineBelongDistributionDTO;
import com.timevale.forward.dal.dto.BugOfflineCountDTO;
import com.timevale.forward.dal.dto.BugOfflineReasonDistributionDTO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOfflineListDO;
import com.timevale.forward.dal.entity.BugOnlineProductLineDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
    int unlinkBugOffline(@Param("bugOfflineDOList")List<BugOfflineDO> bugOfflineDOList);

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

    List<BugOfflineDO> getByIds(@Param("ids") Collection<Long> ids, @Param("containDeleted")Boolean containDeleted);

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
     * @param projectId 项目id
     * @return 项目下每个用户待修复线下bug数量
     */
    List<BugOfflineCountDTO> getBugCount(@Param("projectId") Long projectId);

    /**
     * 查询项目线下bug原因分布
     * @param projectId 项目id
     * @return 线下bug分布列表
     */
    List<BugOfflineReasonDistributionDTO> getReasonDistribution(@Param("projectId") Long projectId);

    /**
     * 查询项目线下bug所属端分布
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

}
