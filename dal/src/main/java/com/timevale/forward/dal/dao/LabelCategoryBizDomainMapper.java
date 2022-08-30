package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.LabelCategoryBizDomainDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 10:41
 */
public interface LabelCategoryBizDomainMapper {


    /**
     *
     * @param labelCategoryIds labelCategoryIds
     * @return 列表
     */
    List<LabelCategoryBizDomainDO> get(@Param("labelCategoryIds") List<Long> labelCategoryIds);

    /**
     * 新增
     *
     * @param labelCategoryBizDomainDO labelCategoryBizDomainDO
     * @return int
     */
    int update(LabelCategoryBizDomainDO labelCategoryBizDomainDO);
    /**
     * 新增类别-业务域
     *
     * @param labelCategoryBizDomainDOList 类别-业务域
     * @return int
     */
    int batchInsert(List<LabelCategoryBizDomainDO> labelCategoryBizDomainDOList);

}
