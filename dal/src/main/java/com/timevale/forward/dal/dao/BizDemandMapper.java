package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandListDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 16:28
 */
public interface BizDemandMapper {

    /**
     * 新增单条信息
     *
     * @param bizDemandDO 业务需求DO
     * @return int
     */
    int insert(BizDemandDO bizDemandDO);

    /**
     * 更新业务需求信息
     *
     * @param bizDemandDO 业务需求DO
     * @return int
     */
    int update(BizDemandDO bizDemandDO);

    /**
     * 更新业务需求信息-完全更新
     *
     * @param bizDemandDO 业务需求DO
     * @return int
     */
    int fullUpdate(BizDemandDO bizDemandDO);

    /**
     * 选择id获取对应业务需求信息
     *
     * @param id id
     * @return 业务需求DO
     */
    BizDemandDO selectById(@Param("id") Long id);

    /**
     * 选择name获取对应业务需求信息
     *
     * @param name 的名字
     * @return 业务需求DO
     */
    BizDemandDO selectByName(@Param("name") String name);

    /**
     * 根据条件查询对应需求，查询列表使用
     *
     * @param bizDemandListCondition 查询条件
     * @return 列表
     */
    List<BizDemandListDO> selectList(BizDemandListCondition bizDemandListCondition);

    /**
     * 根据条件查询对应需求，查询列表使用
     *
     * @param productDemandId 查询条件
     * @return 列表
     */
    List<BizDemandListDO> linkBizDemandList(@Param("productDemandId") Long productDemandId);

    /**
     * 更新业务需求
     *
     * @param ids 产品需求DO
     * @return int
     */
    int updateByIds(@Param("ids") List<Long> ids,@Param("status") Integer status,@Param("retainModifyDate") boolean retainModifyDate);

    /**
     * 更新业务需求
     *
     * @param idList      业务需求id 列表
     * @param createMan   提交人
     * @param createManId 提交人id
     */
    int updateCreateMan(@Param("idList") List<Long> idList, @Param("createMan") String createMan,@Param("createManId") String createManId);

    /**
     * 修改需求提交人
     *
     * @param idList      业务需求id 列表
     * @param submitMan   提交人
     * @param submitManId 提交人id
     */
    int updateSubmitMan(@Param("idList") List<Long> idList, @Param("submitMan") String submitMan,@Param("submitManId") String submitManId);


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
    List<BizDemandDO> selectByIds(@Param("ids") List<Long> ids);

}
