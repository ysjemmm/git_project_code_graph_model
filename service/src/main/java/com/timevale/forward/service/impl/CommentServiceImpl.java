package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.CommentService;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.result.CommentVO;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@Slf4j
@RestService
public class CommentServiceImpl implements CommentService {
    @Override
    public BaseResult<List<CommentVO>> list(CommentQueryList commentQueryList) {
        log.info("项目列表接收参数:{}", commentQueryList);
        return BaseResult.success(Lists.newArrayList(new CommentVO()));
    }


    @Override
    public BaseResult<Boolean> add(CommentAddReq commentAddReq) {
        log.info("项目新增接收参数:{}", commentAddReq);
        return BaseResult.success(true);
    }

}
