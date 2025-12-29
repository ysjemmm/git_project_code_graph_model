package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizDomainGroupRelationDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by qiyuan
 * @date 2025/08/25 16:28
 */
public interface BizDomainGroupRelationMapper {

    List<BizDomainGroupRelationDO> selectAllBizDomainGroupRelation();

    /**
     * 通过业务域id选择
     *
     * @param bizDomainGroupId bizDomainGroupId
     * @return 业务域集关系DO
     */
    List<BizDomainGroupRelationDO> selectByBizDomainGroupId(@Param("bizDomainGroupId") Long bizDomainGroupId);

    /**
     * 通过业务域id选择
     *
     * @param bizDomainId bizDomainId
     * @return 业务域集关系DO
     */
    List<BizDomainGroupRelationDO> selectByBizDomainId(@Param("bizDomainId") Long bizDomainId);

    /**
     * @param bizDomainGroupRelationDOS bizDomainGroupRelationDOS
     * @return int
     */
    int batchInsert(List<BizDomainGroupRelationDO> bizDomainGroupRelationDOS);

    /**
     * @param bizDomainGroupRelationDO bizDomainGroupRelationDO
     * @return int
     */
    int insert(BizDomainGroupRelationDO bizDomainGroupRelationDO);

    int batchDelete(@Param("bizDomainGroupId") Long bizDomainGroupId, @Param("bizDomainIds") List<Long> bizDomainIds);

}
