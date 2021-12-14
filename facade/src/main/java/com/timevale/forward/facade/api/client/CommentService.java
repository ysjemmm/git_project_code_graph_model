package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.result.CommentVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
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
    BaseResult<Integer> add(CommentAddReq commentAddReq);

}
