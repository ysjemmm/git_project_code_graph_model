package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BizDomainCondition;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.facade.api.query.BizDomainQueryList;
import com.timevale.forward.facade.api.request.BizDomainAddReq;
import com.timevale.forward.facade.api.request.BizDomainModifyReq;
import com.timevale.forward.facade.api.result.BizDomainVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:27
 */
@Mapper
public interface BizDomainCopier {
    BizDomainCopier INSTANCE = Mappers.getMapper(BizDomainCopier.class);

    /**
     * 业务域 DO转换VO
     *
     * @param bizDomainDO 业务域DO
     * @return 业务域VO
     */
    BizDomainVO convert(BizDomainDO bizDomainDO);

    /**
     * 业务域 req转换DO
     *
     * @param bizDomainAddReq 业务域req
     * @return 业务域DO
     */
    BizDomainDO convert(BizDomainAddReq bizDomainAddReq);

    /**
     * 业务域 req转换DO
     *
     * @param bizDomainModifyReq 业务域req
     * @return 业务域DO
     */
    BizDomainDO convert(BizDomainModifyReq bizDomainModifyReq);

    /**
     * 批量处理
     *
     * @param bizDomainDOList 业务域Do 列表
     * @return 业务域VO 列表
     */
    List<BizDomainVO> convert(List<BizDomainDO> bizDomainDOList);

    /**
     * 业务域 req转换DO
     *
     * @param bizDomainQueryList 业务域req
     * @return 业务域DO
     */
    BizDomainCondition convert(BizDomainQueryList bizDomainQueryList);
}
