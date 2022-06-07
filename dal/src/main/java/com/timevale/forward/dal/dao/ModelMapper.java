package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ModelDO;
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


}
