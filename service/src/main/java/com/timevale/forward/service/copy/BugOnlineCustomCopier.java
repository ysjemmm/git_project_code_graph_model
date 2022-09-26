package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BugOnlineCustomDO;
import com.timevale.forward.facade.api.request.BugOnlineCustomAddReq;
import com.timevale.forward.facade.api.result.BugOnlineCustomVO;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @Description:
 * @ClassName: BugOnlineCustomCopier
 * @Author yexuan
 * @Date  2022-09-23 15:43
 */
@Mapper
public interface BugOnlineCustomCopier {
    BugOnlineCustomCopier INSTANCE = Mappers.getMapper(BugOnlineCustomCopier.class);

    /**
     * 新增条件转换
     *
     * @param addReqList 业务需求查询条件
     * @return 查询条件
     */
    List<BugOnlineCustomDO> convertList(List<BugOnlineCustomAddReq> addReqList);

    /**
     * 转换
     * @param bugOnlineCustomDOList
     * @return
     */
    List<BugOnlineCustomVO> convertListToVO(List<BugOnlineCustomDO> bugOnlineCustomDOList);


}
