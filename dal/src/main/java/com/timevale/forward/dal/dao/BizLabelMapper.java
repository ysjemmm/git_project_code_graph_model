package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizLabelDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 10:41
 */
public interface BizLabelMapper {

    /**
     * 列表
     *
     * @param labelId labelId
     * @return LabelCategoryDO
     */
    List<BizLabelDO> get(@Param("labelId") Long labelId);

}
