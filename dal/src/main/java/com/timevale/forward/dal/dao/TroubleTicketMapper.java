package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.TroubleTicketDO;
import org.apache.ibatis.annotations.Param;

/**
 * @author by YangXu
 * @date 2022/03/17 10:27
 */
public interface TroubleTicketMapper {

    /**
     * 插入数据
     *
     * @param troubleTicketDO 故障单DO
     * @return 影响行数
     */
    int insert(TroubleTicketDO troubleTicketDO);

    /**
     * 根据id查询
     *
     * @param id 故障工单id
     * @return 故障工单DO
     */
    TroubleTicketDO selectById(@Param("id")Long id);
}
