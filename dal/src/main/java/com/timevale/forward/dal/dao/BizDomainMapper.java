package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BizDomainCondition;
import com.timevale.forward.dal.entity.BizDomainDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 16:28
 */
public interface BizDomainMapper {

    /**
     * 获取业务域列表, ownerId排在前面
     *
     * @return 列表
     */
    List<BizDomainDO> selectWithOrder(@Param("ownerId") String ownerId, @Param("name") String name, @Param("listingStatus") Integer listingStatus);

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
     * @param ids id集合
     * @return 业务域DO 列表
     */
    List<BizDomainDO> getByIds(@Param("ids") Collection<Long> ids);

    /**
     *
     * @param bizDomainDO bizDomainDO
     * @return int
     */
    int insert(BizDomainDO bizDomainDO);

    /**
     *
     * @param bizDomainDO bizDomainDO
     * @return int
     */
    int update(BizDomainDO bizDomainDO);

    /**
     * 获取业务域
     *
     * @param condition condition
     * @return {@link BizDomainDO }
     */
    List<BizDomainDO> selectByCondition(BizDomainCondition condition);

    List<BizDomainDO> simpleList(BizDomainCondition condition);

}
