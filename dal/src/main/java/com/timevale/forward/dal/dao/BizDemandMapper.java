package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDomainDO;

import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/15 10:49
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
     * 选择id获取对应业务需求信息
     *
     * @param id id
     * @return 业务需求DO
     */
    BizDemandDO selectById(Long id);

    /**
     * 根据条件查询对应需求
     *
     * @param bizDemandListCondition 查询条件
     * @return 列表
     */
    List<BizDemandDO> select(BizDemandListCondition bizDemandListCondition);

}
