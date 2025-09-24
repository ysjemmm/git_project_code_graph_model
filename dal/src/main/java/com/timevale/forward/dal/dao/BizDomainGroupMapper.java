package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BizDomainGroupCondition;
import com.timevale.forward.dal.condition.BizDomainGroupMatchCondition;
import com.timevale.forward.dal.entity.BizDomainGroupDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author by qiyuan
 * @date 2025/08/25 16:28
 */
public interface BizDomainGroupMapper {

    /**
     * 获取业务域集列表
     *
     * @return 列表
     */
    List<BizDomainGroupDO> selectAllBizDomainGroup();

    List<BizDomainGroupDO> matchBizDomainGroupList(BizDomainGroupMatchCondition bizDomainGroupMatchCondition);

    /**
     * 通过id选择
     *
     * @param id id
     * @return 业务域集DO
     */
    BizDomainGroupDO selectById(@Param("id") Long id);

    /**
     * 选择 by 名字
     *
     * @param name 名字
     * @return 业务域集DO
     */
    BizDomainGroupDO selectByName(@Param("name") String name);

    /**
     * 通过id选择
     *
     * @param ids id集合
     * @return 业务域集DO 列表
     */
    List<BizDomainGroupDO> getByIds(@Param("ids") Collection<Long> ids);

    /**
     *
     * @param bizDomainGroupDO bizDomainGroupDO
     * @return int
     */
    int insert(BizDomainGroupDO bizDomainGroupDO);

    /**
     *
     * @param bizDomainGroupDO bizDomainGroupDO
     * @return int
     */
    int update(BizDomainGroupDO bizDomainGroupDO);

    /**
     * 获取业务域集
     *
     * @param condition condition
     * @return {@link BizDomainGroupDO }
     */
    List<BizDomainGroupDO> selectByCondition(BizDomainGroupCondition condition);

    int countProductLine(@Param("bizDomainGroupId") Long bizDomainGroupId, @Param("ownerId") String ownerId);
    int countBizGroup(@Param("bizDomainGroupId") Long bizDomainGroupId, @Param("ownerId") String ownerId);

}
