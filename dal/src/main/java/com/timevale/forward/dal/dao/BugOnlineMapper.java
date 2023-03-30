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
    BugOnlineDO selectById(@Param("id") Long id);

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
    void update(@Param("bugOnlineDO") BugOnlineDO bugOnlineDO);

    /**
     * 完整更新
     *
     * @param bugOnlineDO 线上bugDO
     */
    void fullUpdate(@Param("bugOnlineDO") BugOnlineDO bugOnlineDO);

    /**
     * 插入线上bug
     *
     * @param bugOnlineDO 参数
     * @return Long
     */
    Long insert(@Param("bugOnlineDO") BugOnlineDO bugOnlineDO);

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
    List<BugOnlineDO> selectByIds(@Param("ids") Collection<Long> ids,@Param("containDeleted")Boolean containDeleted);

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


    /**
     * 根据客户id查询
     */
    List<BugOnlineListDO> getByCustomId(@Param("customId") Long customId);

    /**
     * 根据客户id集合查询关联的线上bug数据
     *
     * @param customerIds 客户id集合
     * @return 线上bug数据集合
     */
    List<BugOnlineDO> getByCustomerIds(@Param("customerIds") Collection<Long> customerIds);
}
