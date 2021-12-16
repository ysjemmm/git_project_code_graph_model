package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.entity.PersonDO;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 11:40
 */
public interface PersonMapper {

    /**
     * 批量插入人员信息
     *
     * @param list 列表
     * @return int
     */
    int inserts(List<PersonDO> list);


    /**
     * 获取人员信息
     *
     * @param personListCondition 查询条件
     * @return 列表
     */
    List<PersonDO> select(PersonListCondition personListCondition);

}
