package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.LabelCategoryQueryList;
import com.timevale.forward.facade.api.query.LabelInCategoryQueryList;
import com.timevale.forward.facade.api.request.LabelCategoryAddReq;
import com.timevale.forward.facade.api.result.LabelCategorySimpleVO;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface LabelCategoryService {



    /**
     * 埋点地图
     *
     * @return 埋点属性
     */
    BaseResult<PageQueryResult<TrackPropVO>> list(LabelCategoryQueryList labelCategoryQueryList);

    /**
     * 埋点地图
     *
     * @return 埋点属性
     */
    BaseResult<List<LabelCategorySimpleVO>> getLabelInCategory(LabelInCategoryQueryList labelInCategoryQueryList);


    /**
     * 新增
     *
     * @param labelCategoryAddReq 标签类别新增
     * @return Boolean
     */
    BaseResult<Boolean> add(LabelCategoryAddReq labelCategoryAddReq);


}
