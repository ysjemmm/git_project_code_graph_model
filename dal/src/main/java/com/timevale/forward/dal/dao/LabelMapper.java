package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.LabelCategoryListCondition;
import com.timevale.forward.dal.entity.LabelDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 10:41
 */
public interface LabelMapper {

    /**
     * 新增
     *
     * @param labelDO labelDO
     * @return int
     */
    int insert(LabelDO labelDO);

    /**
     * 新增
     *
     * @param labelDO labelDO
     * @return int
     */
    int update(LabelDO labelDO);

    /**
     * 列表
     *
     * @return LabelCategoryDO
     */
    List<LabelDO> list(LabelCategoryListCondition condition);

    /**
     * 列表
     *
     * @param id id
     * @return LabelCategoryDO
     */
    LabelDO get(@Param("id") Long id);

}
