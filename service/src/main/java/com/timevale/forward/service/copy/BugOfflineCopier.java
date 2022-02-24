package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BugOfflineListCondition;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOfflineListDO;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.BugOfflineAddReq;
import com.timevale.forward.facade.api.result.BugOfflineDetailVO;
import com.timevale.forward.facade.api.result.BugOfflineVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2022/02/23 17:53
 */
@Mapper
public interface BugOfflineCopier {

    BugOfflineCopier INSTANCE = Mappers.getMapper(BugOfflineCopier.class);

    /**
     * 转换转换DO
     *
     * @param bugOfflineQueryList 对象
     * @return BugOfflineListCondition
     */
    BugOfflineListCondition convert(BugOfflineQueryList bugOfflineQueryList);

    /**
     * 转换转换DO
     *
     * @param bugOfflineAddReq 对象
     * @return BugOfflineDO
     */
    BugOfflineDO convert(BugOfflineAddReq bugOfflineAddReq);


    /**
     * 转换转换DO
     *
     * @param bugOfflineDO 对象
     * @return BugOfflineVO
     */
    BugOfflineVO convert(BugOfflineDO bugOfflineDO);

    /**
     * BugOfflineDO  -->  BugOfflineDetailVO
     *
     * @param  bugOfflineDO 参数
     * @return BugOfflineDetailVO
     * */
    BugOfflineDetailVO transform(BugOfflineDO bugOfflineDO);

    /**
     * 转换转换DO
     *
     * @param bugOfflineListDO 对象
     * @return BugOfflineVO
     */
    BugOfflineVO convert(BugOfflineListDO bugOfflineListDO);


}



































