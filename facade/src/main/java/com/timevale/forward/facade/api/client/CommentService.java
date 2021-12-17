package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.result.CommentVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface CommentService {
    /**
     * 查列表
     *
     * @param commentQueryList 评论信息
     * @return 列表
     */
    BaseResult<List<CommentVO>> list(CommentQueryList commentQueryList);

    /**
     * 新增
     *
     * @param commentAddReq 评论信息
     * @return 数量
     */
    BaseResult<Boolean> add(CommentAddReq commentAddReq);

}
