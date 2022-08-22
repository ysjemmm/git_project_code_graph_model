package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.LabelListCondition;
import com.timevale.forward.dal.entity.LabelDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 10:41
 */
public interface LabelMapper {


    /**
     *
     * @param labelDOList labelDOList
     * @return int
     */
    int batchInsert(List<LabelDO> labelDOList);

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
    List<LabelDO> list(LabelListCondition condition);

    /**
     * 列表
     *
     * @param id id
     * @return LabelCategoryDO
     */
    LabelDO get(@Param("id") Long id);

    /**
     * 列表
     *
     * @return LabelCategoryDO
     */
    List<LabelDO> getByCategoryIds(@Param("categoryIds")List<Long> categoryIds,@Param("containDeleted")Boolean containDeleted);

    /**
     * 列表
     *
     * @return LabelCategoryDO
     */
    List<LabelDO> getByNameInOneCategory(@Param("names")List<String> names,@Param("categoryId") Long categoryId);

    /**
     * 列表
     *
     * @return LabelCategoryDO
     */
    List<LabelDO> getByIds(@Param("ids")List<Long> ids);


}
