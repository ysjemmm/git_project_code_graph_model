package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizDemandCustomDO;
import com.timevale.forward.facade.api.request.BizDemandCustomAddReq;

import java.util.List;

/**
 * @author yexuan
 * @date 2022-09-23 15:26 yexuan
 */
public interface BizDemandCustomComponent {

    /**
     * 新增
     * @param addReqList
     */
    void add(List<BizDemandCustomAddReq>  addReqList,Long bizDemandId);

    /**
     * 更新
     * @param addReqList
     * @param bizDemandId
     */
    void update(List<BizDemandCustomAddReq>  addReqList,Long bizDemandId);


    /**
     * 根据业务需求id查询
     * @param bizDemandId
     * @return
     */
    List<BizDemandCustomDO> selectByBizDemandId(Long bizDemandId);
}
