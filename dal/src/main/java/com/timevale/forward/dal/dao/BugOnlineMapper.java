package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BugOnlineListCondition;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.BugOnlineListDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * @Date 2022/3/18 11:14
 * @Author 望轩
 */
public interface BugOnlineMapper {
    /**
     * 根据线上bug的id查询线上bug
     *
     * @param id 线上bug的id
     * @return 返回值
     */
    BugOnlineDO get(@Param("id") Long id);

    /**
     * 选择所有id
     *
     * @return {@link List}<{@link Long}>
     */
    List<Long> selectAllId();

    /**
     * 选择列表通过条件
     *
     * @param bugOnlineListCondition 线上bug列表条件
     * @return BugOnlineListDO 列表
     */
    List<BugOnlineListDO> selectListByCondition(BugOnlineListCondition bugOnlineListCondition);

    /**
     * 更新线上bug
     *
     * @param bugOnlineDO 参数
     */
    void update(BugOnlineDO bugOnlineDO);

    /**
     * 插入线上bug
     *
     * @param bugOnlineDO 参数
     */
    void insert(BugOnlineDO bugOnlineDO);

    /**
     * 根据线上bug的id查询线上bug
     *
     * @param linkBugId 关联的线上bug的id
     * @return 返回值
     */
    List<BugOnlineDO> selectByLinkBugId(@Param("linkBugId") Long linkBugId);

    /**
     * 批量更新关联的bug
     * @param ids ids
     * @param linkBugId linkBugId
     * @return int
     */
    int updateByIds(@Param("ids") List<Long> ids,@Param("linkBugId") Long linkBugId);

    /**
     * 根据线上bug的id查询线上bug
     *
     * @param ids 线上bug的id
     * @return 返回值
     */
    List<BugOnlineDO> getByIds(@Param("ids") Collection<Long> ids, @Param("containDeleted")Boolean containDeleted);

    /**
     * 根据线上bug的业务需求id查询线上bug
     *
     * @param name name
     * @return 返回值
     */
    List<BugOnlineDO> selectByName(@Param("name") String name);

    /**
     * 查询线上bug
     *
     * @param status status
     * @return 返回值
     */
    List<BugOnlineDO> selectByStatus(@Param("status") List<Integer> status);

    /**
     * 批量更新关联的bug
     * @param ids ids
     * @param status status
     * @return int
     */
    int updateStatusByIds(@Param("ids") List<Long> ids,@Param("status") Integer status);

    @Select("select count(*) from bug_online where biz_id = #{bizId} and is_deleted = false")
    boolean bizIdExists(@Param("bizId") String bizId);

    void delete(@Param("id")Long id);

    /**
     * 查询关联的该线下bug的线上bug
     *
     * @param bugOfflineId 线下bug id
     * @return 线上bug数据集合
     */
    List<BugOnlineDO> getByBugOffline(@Param("bugOfflineId")Long bugOfflineId);

    /**
     * 清空关联的线下bug
     *
     * @param ids 线上bug id集合
     */
    void clearBugOffline(@Param("ids")Collection<Long> ids);
    List<BugOnlineListDO> getByCustomId(@Param("customId") Long customId);

    /**
     * 根据客户id集合查询关联的线上bug数据
     *
     * @param customerIds 客户id集合
     * @return 线上bug数据集合
     */
    List<BugOnlineDO> getByCustomerIds(@Param("customerIds") Collection<Long> customerIds);

    void updateConvertBizStatus(@Param("id")Long id, @Param("convertBizStatus")Integer convertBizStatus);

    /**
     * 打开次数加一
     */
    void updateIncOpenCount(@Param("id") Long id);

    @Select("SELECT * FROM bug_online WHERE operator_id=#{operatorId} AND is_deleted = false")
    List<BugOnlineDO> getByOperatorId(@Param("operatorId") String operatorId);

    @Select("SELECT * FROM bug_online WHERE proposer_id=#{proposerId} AND is_deleted = false")
    List<BugOnlineDO> getByProposerId(@Param("proposerId") String proposerId);

    void updateOperator(@Param("ids") Collection<Long> ids, @Param("operatorId") String operatorId, @Param("operator") String operator);

    void updateProposer(@Param("ids") Collection<Long> ids, @Param("proposerId") String proposerId, @Param("proposer") String proposer);
}
