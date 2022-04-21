package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.query.SearchConditionQueryList;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.request.SearchConditionAddReq;
import com.timevale.forward.facade.api.result.CommentVO;
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
     * @param searchConditionQueryList 搜索条件查询列表
     */
    BaseResult<List<SearchConditionVO>> list(SearchConditionQueryList searchConditionQueryList);

    /**
     * 新增
     *
     * @param searchConditionAddReq 评论信息
     */
    BaseResult<Boolean> add(SearchConditionAddReq searchConditionAddReq);

}
