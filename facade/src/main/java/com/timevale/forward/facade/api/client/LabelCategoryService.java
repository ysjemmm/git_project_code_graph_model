package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.LabelCategoryQueryList;
import com.timevale.forward.facade.api.query.LabelInCategoryQueryList;
import com.timevale.forward.facade.api.request.LabelCategoryAddReq;
import com.timevale.forward.facade.api.request.LabelCategoryModifyReq;
import com.timevale.forward.facade.api.result.LabelCategoryDetailVO;
import com.timevale.forward.facade.api.result.LabelCategorySimpleVO;
import com.timevale.forward.facade.api.result.LabelCategoryVO;
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
     * 列表
     *
     * @return 列表
     */
    BaseResult<PageQueryResult<LabelCategoryVO>> list(LabelCategoryQueryList labelCategoryQueryList);

    /**
     * 所有类别
     *
     * @return 所有类别
     */
    BaseResult<List<LabelCategorySimpleVO>> getAll();

    /**
     * 类别下-标签
     *
     * @return 类别下-标签
     */
    BaseResult<List<LabelCategorySimpleVO>> getLabelInCategory(LabelInCategoryQueryList labelInCategoryQueryList);

    /**
     * 查看
     *
     * @param categoryId categoryId
     * @return 详情信息
     */
    BaseResult<LabelCategoryDetailVO> get(Long categoryId);

    /**
     * 查看
     *
     * @param categoryId categoryId
     * @return 详情信息
     */
    BaseResult<Boolean> delete(Long categoryId);


    /**
     * 新增
     *
     * @param labelCategoryAddReq 标签类别新增
     * @return Boolean
     */
    BaseResult<Boolean> add(LabelCategoryAddReq labelCategoryAddReq);

    /**
     * 新增
     *
     * @param labelCategoryModifyReq 标签类别新增
     * @return Boolean
     */
    BaseResult<Boolean> modify(LabelCategoryModifyReq labelCategoryModifyReq);


}
