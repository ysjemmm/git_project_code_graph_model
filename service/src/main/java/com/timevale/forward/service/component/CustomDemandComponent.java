package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.CustomDemandListCondition;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
public interface CustomDemandComponent {

    /**
     * 更新业务需求状态根据关联的产品需求
     *
     * @param customDemandId 业务需求id
     */
    void updateStatusBaseOnProductDemand(Long customDemandId);


    /**
     * 客户需求
     *
     * @return 客户需求
     */
    BaseResult<PageQueryResult<CustomDemandVO>> list(CustomDemandListCondition condition);


    /**
     * 得到客户需求，关联的产品需求，关联的项目的发布时间
     *
     * @param customDemandId 业务需求id
     * @return 项目发布时间
     */
    Date getProjectEndDate(Long customDemandId);

    /**
     *
     * @param customDemandId customDemandId
     */
    void updateProjectEndDate(Long customDemandId);

}
