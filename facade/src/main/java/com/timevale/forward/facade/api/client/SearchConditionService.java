package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.SearchConditionQueryList;
import com.timevale.forward.facade.api.request.SearchConditionAddReq;
import com.timevale.forward.facade.api.request.SearchConditionDeleteReq;
import com.timevale.forward.facade.api.request.SearchConditionModifyReq;
import com.timevale.forward.facade.api.result.SearchConditionVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/04/21 17:09
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface SearchConditionService {
    /**
     * 查询列表
     *
     * @param searchConditionQueryList 搜索条件-查询列表
     */
    BaseResult<List<SearchConditionVO>> list(SearchConditionQueryList searchConditionQueryList);

    /**
     * 新增
     *
     * @param searchConditionAddReq 搜索条件-添加请求
     */
    BaseResult<Boolean> add(SearchConditionAddReq searchConditionAddReq);

    /**
     * 新增
     *
     * @param searchConditionModifyReq 搜索条件-修改请求
     */
    BaseResult<Boolean> update(SearchConditionModifyReq searchConditionModifyReq);

    /**
     * 新增
     *
     * @param searchConditionDeleteReq 搜索条件-删除请求
     */
    BaseResult<Boolean> delete(SearchConditionDeleteReq searchConditionDeleteReq);
}
