package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ModelCondition;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface ModelMapper {

    /**
     * 获取产品线列表
     *
     * @return 列表
     */
    List<ModelDO> selectAllModel();

    /**
     * 获取产品线列表
     *
     * @return 列表
     */
    ModelDO get(@Param("id") Long id);

    /**
     * 获取产品线列表
     *
     * @return 列表
     */
    List<ModelDO> getByIds(@Param("ids") List<Long> ids);

    /**
     *
     * @param productLineIds productLineIds
     * @return 列表
     */
    List<ModelDO> getByProductLineId(@Param("productLineIds") List<Long> productLineIds);

    /**
     *
     * @param modelDO modelDO
     * @return int
     */
    int insert(ModelDO modelDO);


    /**
     *
     * @param modelDO modelDO
     * @return int
     */
    int update(ModelDO modelDO);


    /**
     * 获取模块
     *
     * @param condition condition
     * @return {@link ProductLineDO }
     */
    List<ModelDO> selectByCondition(ModelCondition condition);

}
