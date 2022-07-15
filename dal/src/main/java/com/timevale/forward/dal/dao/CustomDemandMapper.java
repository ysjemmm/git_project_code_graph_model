package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.CustomDemandListCondition;
import com.timevale.forward.dal.entity.CustomDemandDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 16:28
 */
public interface CustomDemandMapper {

    /**
     * 新增单条信息
     *
     * @param customDemandDO 需求DO
     * @return int
     */
    int insert(CustomDemandDO customDemandDO);

    /**
     * 更新需求信息
     *
     * @param customDemandDO 需求DO
     * @return int
     */
    int update(CustomDemandDO customDemandDO);

    /**
     * 更新需求信息
     *
     * @param customDemandDO 需求DO
     * @return int
     */
    int fullUpdate(CustomDemandDO customDemandDO);

    /**
     * 选择id获取对应需求信息
     *
     * @param id id
     * @return 需求DO
     */
    CustomDemandDO selectById(@Param("id") Long id);


    /**
     * 根据条件查询对应需求，查询列表使用
     *
     * @param condition 查询条件
     * @return 列表
     */
    List<CustomDemandDO> list(CustomDemandListCondition condition);

    /**
     * 选择id获取对应需求信息
     *
     * @param productDemandId productDemandId
     * @return 需求DO
     */
    List<CustomDemandDO> linkCustomDemandList(@Param("productDemandId") Long productDemandId);

    /**
     * 修改需求接收人
     *
     * @param idList       业务需求id 列表
     * @param receiveMan   接收人
     * @param receiveManId 接收人id
     */
    int updateReceiveMan(@Param("idList") List<Long> idList, @Param("receiveMan") String receiveMan,@Param("receiveManId") String receiveManId);

    /**
     * 选择id获取对应业务需求信息
     *
     * @param ids id
     * @return 业务需求DO
     */
    List<CustomDemandDO> selectByIds(@Param("ids") List<Long> ids);

}
