package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.entity.PersonDO;
import org.apache.ibatis.annotations.Param;

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
    int inserts(@Param("list") List<PersonDO> list);

    /**
     * 插入人员信息
     *
     * @param personDO 列表
     * @return int
     */
    int insert(PersonDO personDO);


    /**
     * 获取人员信息
     *
     * @param personListCondition 查询条件
     * @return 列表
     */
    List<PersonDO> select(PersonListCondition personListCondition);

    /**
     * 删除
     *
     * @param personDO 人员信息
     * @return int
     */
    int update(PersonDO personDO);

    /**
     * 获取人员信息
     *
     * @param userIds 花名
     * @param mainIds 项目id
     * @param type    产品经理or团队成员or执行人
     * @return list
     */
    List<Long> getMainIds(@Param("userIds") List<String> userIds, @Param("mainIds") List<Long> mainIds, @Param("type") Integer type);


    /**
     * 获取人员信息
     *
     * @param mainIds 项目id
     * @param type    产品经理or团队成员
     * @return list
     */
    List<PersonDO> get(@Param("mainIds") List<Long> mainIds, @Param("type") Integer type);


    /**
     * 获取人员信息
     *
     * @param userIds 花名
     * @param mainIds 项目id
     * @param type    产品经理or团队成员or执行人
     * @return list
     */
    List<PersonDO> getPersons(@Param("userIds") List<String> userIds, @Param("mainIds") List<Long> mainIds, @Param("type") Integer type);
}
