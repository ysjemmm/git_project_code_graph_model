package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizDomainDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 16:28
 */
public interface BizDomainMapper {

    /**
     * 获取业务域列表
     *
     * @return 列表
     */
    List<BizDomainDO> selectAllBizDomain();

    /**
     * 通过id选择
     *
     * @param id id
     * @return 业务域DO
     */
    BizDomainDO selectById(@Param("id") Long id);

    /**
     * 选择 by 名字
     *
     * @param nameList 名字列表
     * @return 业务域DO
     */
    List<BizDomainDO> selectByName(@Param("nameList") List<String> nameList);

    /**
     * 通过id选择
     *
     * @param idList id列表
     * @return 业务域DO 列表
     */
    List<BizDomainDO> selectByIdList(@Param("idList") List<Long> idList);
}
