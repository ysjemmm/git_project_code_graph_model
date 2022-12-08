package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BugOnlineStatusOperatorDO;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @Description:
 * @ClassName: BugOnlineStatusOperatorMapper
 * @Author yexuan
 * @Date  2022-12-08 14:23
 */
public interface BugOnlineStatusOperatorMapper {


    /**
     * 插入单条数据
     *
     * @param bugOnlineStatusOperatorDO 参数
     */
    void insert(@Param("bugOnlineStatusOperatorDO") BugOnlineStatusOperatorDO bugOnlineStatusOperatorDO);

    /**
     * 根据日志状态变更集合批量删除数据
     *
     * @param bugOnlineId
     */
    void deleteByBugOnlineId(@Param("bugOnlineId") Long bugOnlineId);

    /**
     * 根据线上Bug和状态获取
     * @param bugOnlineId
     * @param status
     * @return
     */
    @Select("select id from bug_online_status_operator where bug_online_id=#{bugOnlineId} and status=#{status} and is_deleted=0")
    Long getByBugOnlineIdAndStatus(@Param("bugOnlineId") Long bugOnlineId,@Param("status") Integer status);


    /**
     * 更新操作人
     * @param operatorId
     * @param operator
     * @param id
     */
    @Update("update bug_online_status_operator set operator_id=#{operatorId},operator=#{operator} where id=#{id}")
    void updateOperatorById(@Param("operatorId") String operatorId, @Param("operator") String operator, @Param("id") Long id);
}
