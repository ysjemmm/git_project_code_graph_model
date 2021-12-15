package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizDomainDO;

import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/15 10:36
 */
public interface BizDomainMapper {

    /**
     * 获取业务域列表
     *
     * @return 列表
     */
    List<BizDomainDO> selectList();
}
