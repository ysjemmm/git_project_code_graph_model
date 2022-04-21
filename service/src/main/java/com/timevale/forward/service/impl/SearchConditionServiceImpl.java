package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.CommentDO;
import com.timevale.forward.facade.api.client.CommentService;
import com.timevale.forward.facade.api.client.SearchConditionService;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.query.SearchConditionQueryList;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.request.SearchConditionAddReq;
import com.timevale.forward.facade.api.result.CommentVO;
import com.timevale.forward.facade.api.result.SearchConditionVO;
import com.timevale.forward.model.enums.CommentTypeEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.CommentCopier;
import com.timevale.forward.service.observer.event.CommentMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author by YangXu
 * @date 2022/04/21 17:10
 */
@Slf4j
@LogPoint
@RestService
public class SearchConditionServiceImpl implements SearchConditionService {

    @Override
    public BaseResult<List<SearchConditionVO>> list(SearchConditionQueryList searchConditionQueryList) {
        return BaseResult.success(new ArrayList<>());
    }

    @Override
    public BaseResult<Boolean> add(SearchConditionAddReq searchConditionAddReq) {
        return BaseResult.success(true);
    }
}
