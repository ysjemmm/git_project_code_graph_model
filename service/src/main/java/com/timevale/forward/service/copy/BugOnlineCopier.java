package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BugOnlineListCondition;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.BugOnlineListDO;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.BugOnlineAddReq;
import com.timevale.forward.facade.api.request.BugOnlineModifyReq;
import com.timevale.forward.facade.api.result.BugOnlineDetailVO;
import com.timevale.forward.facade.api.result.BugOnlineVO;
import com.timevale.forward.model.middle.BugOnlineMD;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @Date 2022/3/18 13:54
 * @Author 望轩
 */
@Mapper
public interface BugOnlineCopier {
    BugOnlineCopier INSTANCE = Mappers.getMapper(BugOnlineCopier.class);

    /**
     * BugOnlineDO --> BugOnlineDetailVO
     *
     * @param bugOnlineDO 对象
     * @return BugOfflineListCondition
     */
    BugOnlineDetailVO convert(BugOnlineDO bugOnlineDO);

    /**
     * BugOnlineListDO --> BugOnlineVO
     *
     * @param bugOnlineListDO 对象
     * @return BugOnlineVO
     */
    BugOnlineVO convert(BugOnlineListDO bugOnlineListDO);

    /**
     * bugOnlineDO --> BugOnlineVO
     *
     * @param bugOnlineDO 对象
     * @return BugOnlineVO
     */
    BugOnlineVO convertT(BugOnlineDO bugOnlineDO);

    /**
     * bugOnlineQueryList --> BugOnlineListCondition
     *
     * @param bugOnlineQueryList 对象
     * @return BugOnlineListCondition
     */
    BugOnlineListCondition convert(BugOnlineQueryList bugOnlineQueryList);

    /**
     * BugOnlineAddReq --> BugOnlineDO
     *
     * @param bugOnlineAddReq 对象
     * @return BugOnlineDO
     */
    BugOnlineDO transfer(BugOnlineAddReq bugOnlineAddReq);

    /**
     * BugOnlineModifyReq --> BugOnlineDO
     *
     * @param bugOnlineModifyReq 参数
     * @return 返回参数
     */
    BugOnlineDO change(BugOnlineModifyReq bugOnlineModifyReq);

    /**
     * BugOnlineModifyReq --> BugOnlineMD
     *
     * @param bugOnlineModifyReq 参数
     * @return 返回值
     */
    BugOnlineMD convert(BugOnlineModifyReq bugOnlineModifyReq);

    /**
     * BugOnlineDO --> BugOnlineMD
     *
     * @param bugOnlineDO 参数
     * @return 返回值
     */
    BugOnlineMD change(BugOnlineDO bugOnlineDO);

    /**
     *
     * @param bugOnlineListDOList 参数
     * @return 返回值
     */
    List<BugOnlineVO> convert(List<BugOnlineListDO> bugOnlineListDOList);
}