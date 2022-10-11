package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BizDemandCustomDO;
import com.timevale.forward.facade.api.request.BizDemandCustomAddReq;
import com.timevale.forward.facade.api.result.BizDemandCustomVO;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @Description:
 * @ClassName: BizDemandCustomCopier
 * @Author yexuan
 * @Date  2022-09-23 15:43
 */
@Mapper
public interface BizDemandCustomCopier {
    BizDemandCustomCopier INSTANCE = Mappers.getMapper(BizDemandCustomCopier.class);

    /**
     * 新增条件转换
     *
     * @param addReqList 业务需求查询条件
     * @return 查询条件
     */
    List<BizDemandCustomDO> convertList(List<BizDemandCustomAddReq> addReqList);

    /**
     * 转换
     * @param bizDemandCustomDOList
     * @return
     */
    List<BizDemandCustomVO> convertListToVO(List<BizDemandCustomDO> bizDemandCustomDOList);


}
