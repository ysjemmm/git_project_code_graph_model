package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandAddReq;
import com.timevale.forward.facade.api.request.BizDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.BizDomainVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/15 10:56
 */
@Mapper
public interface BizDemandCopier {
    BizDemandCopier INSTANCE = Mappers.getMapper(BizDemandCopier.class);

    /**
     * 查询条件转换
     *
     * @param bizDemandQueryList 业务需求查询条件
     * @return 查询条件
     */
    BizDemandListCondition convert(BizDemandQueryList bizDemandQueryList);


    /**
     * 请求添加转换为DO
     *
     * @param bizDemandAddReq 业务需求添加
     * @return 业务需求DO
     */
    BizDemandDO convert(BizDemandAddReq bizDemandAddReq);


    /**
     * 请求修改转换为DO
     *
     * @param bizDemandModifyReq 业务需求修改要求的事情
     * @return
     */
    BizDemandDO convert(BizDemandModifyReq bizDemandModifyReq);

    /**
     * 业务需求DO转换为VO
     *
     * @param bizDemandDO 业务需求DO
     * @return 业务需求详细VO
     */
    BizDemandDetailVO convert(BizDemandDO bizDemandDO);


    /**
     * DO批量转换为VO
     *
     * @param list 列表
     * @return 业务需求列表
     */
    List<BizDemandVO> convert(List<BizDemandDO> list);
}
