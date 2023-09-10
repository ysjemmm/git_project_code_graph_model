package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizTimeDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;


/**
 * 业务时间文件夹
 *
 * @author yangxu
 * @date 2023/09/10
 */
public interface BizTimeMapper {

    @Delete("DELETE FROM biz_time WHERE true")
    void deleteAll();

    void batchInsert(@Param("bizTimes") Collection<BizTimeDO> bizTimes);
}
