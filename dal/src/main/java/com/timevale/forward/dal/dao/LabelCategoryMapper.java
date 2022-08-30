package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.LabelCategoryListCondition;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 10:41
 */
public interface LabelCategoryMapper {

    /**
     * 新增
     *
     * @param labelCategoryDO labelCategoryDO
     * @return int
     */
    int insert(LabelCategoryDO labelCategoryDO);

    /**
     * 新增
     *
     * @param labelCategoryDO labelCategoryDO
     * @return int
     */
    int update(LabelCategoryDO labelCategoryDO);

    /**
     * 列表
     *
     * @return LabelCategoryDO
     */
    List<LabelCategoryDO> list(LabelCategoryListCondition condition);


    /**
     * 列表
     *
     * @return LabelCategoryDO
     */
    List<LabelCategoryDO> getAll();

    /**
     * 列表
     *
     * @param ids ids
     * @return LabelCategoryDO
     */
    List<LabelCategoryDO> get(@Param("ids") List<Long> ids);

    /**
     * 列表
     *
     * @return LabelCategoryDO
     */
    List<LabelCategoryDO> getByName(@Param("name") String name);

}
