package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BizDomainGroupCondition;
import com.timevale.forward.dal.condition.BizDomainGroupMatchCondition;
import com.timevale.forward.dal.entity.BizDomainGroupDO;
import com.timevale.forward.facade.api.query.BizDomainGroupMatchQueryList;
import com.timevale.forward.facade.api.query.BizDomainGroupQueryList;
import com.timevale.forward.facade.api.request.BizDomainGroupAddReq;
import com.timevale.forward.facade.api.request.BizDomainGroupModifyReq;
import com.timevale.forward.facade.api.result.BizDomainGroupVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by qiyuan
 * @date 2025/08/25 10:27
 */
@Mapper
public interface BizDomainGroupCopier {
    BizDomainGroupCopier INSTANCE = Mappers.getMapper(BizDomainGroupCopier.class);

    /**
     * 业务域集 DO转换VO
     *
     * @param bizDomainGroupDO 业务域集DO
     * @return 业务域集VO
     */
    BizDomainGroupVO convert(BizDomainGroupDO bizDomainGroupDO);

    /**
     * 业务域集 req转换DO
     *
     * @param bizDomainGroupAddReq 业务域集req
     * @return 业务域集DO
     */
    BizDomainGroupDO convert(BizDomainGroupAddReq bizDomainGroupAddReq);

    /**
     * 业务域集 req转换DO
     *
     * @param bizDomainGroupModifyReq 业务域req
     * @return 业务域DO
     */
    BizDomainGroupDO convert(BizDomainGroupModifyReq bizDomainGroupModifyReq);

    /**
     * 批量处理
     *
     * @param bizDomainGroupDOList 业务域Do 列表
     * @return 业务域VO 列表
     */
    List<BizDomainGroupVO> convert(List<BizDomainGroupDO> bizDomainGroupDOList);

    /**
     * 业务域集 req转换DO
     *
     * @param bizDomainGroupQueryList 业务域req
     * @return 业务域集DO
     */
    BizDomainGroupCondition convert(BizDomainGroupQueryList bizDomainGroupQueryList);

    BizDomainGroupMatchCondition convert(BizDomainGroupMatchQueryList bizDomainGroupMatchQueryList);
}
