package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BugStatusOperatorDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Date 2022/3/18 18:12
 * @Author 望轩
 */
public interface BugStatusOperatorMapper {

    /**
     * 通过bug的id批量查询状态经办人记录
     *
     * @param bugIds bug的id集合
     * @return 返回结果
     */
    List<BugStatusOperatorDO> batchSelectByBugIds(@Param("bugIds") List<Integer> bugIds);

    /**
     * 插入单条数据
     *
     * @param bugStatusOperatorDO 参数
     * */
    void insert(@Param("bugStatusOperatorDO") BugStatusOperatorDO bugStatusOperatorDO);
}
