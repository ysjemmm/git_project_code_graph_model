package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizDemandCustomDO;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description:
 * @ClassName: BizDemandCustomMapper
 * @Author yexuan
 * @Date  2022-09-23 15:18
 */
public interface BizDemandCustomMapper {

    /**
     * 批量插入数据
     *
     * @param bizDemandCustomDOList 参数
     */
    void batchInsert(@Param("bizDemandCustomDOList") List<BizDemandCustomDO> bizDemandCustomDOList);

    /**
     * 逻辑删除
     * @param bizDemandCustomDO
     */
    void delete(@Param("bizDemandCustomDO") BizDemandCustomDO bizDemandCustomDO);

    /**
     * 根据业务需求id查询
     * @param bizDemandId
     * @return
     */
    List<BizDemandCustomDO> selectByBizDemandId(@Param("bizDemandId") Long bizDemandId);
}
