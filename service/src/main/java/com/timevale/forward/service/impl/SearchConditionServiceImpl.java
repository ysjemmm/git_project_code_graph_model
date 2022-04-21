package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.SearchConditionMapper;
import com.timevale.forward.dal.entity.SearchConditionDO;
import com.timevale.forward.facade.api.client.SearchConditionService;
import com.timevale.forward.facade.api.query.SearchConditionQueryList;
import com.timevale.forward.facade.api.request.SearchConditionAddReq;
import com.timevale.forward.facade.api.result.SearchConditionVO;
import com.timevale.forward.service.copy.SearchConditionCopier;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
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

    @Resource
    SearchConditionMapper searchConditionMapper;

    @Override
    public BaseResult<List<SearchConditionVO>> list(SearchConditionQueryList searchConditionQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        Integer model = searchConditionQueryList.getModel();
        Integer tabType = searchConditionQueryList.getTabType();

        List<SearchConditionDO> select = searchConditionMapper.select(model, tabType, userInfo.getId());
        List<SearchConditionVO> searchConditionVOList = select.stream().map(SearchConditionCopier.INSTANCE::convert).collect(Collectors.toList());

        return BaseResult.success(searchConditionVOList);
    }

    @Override
    public BaseResult<Boolean> add(SearchConditionAddReq searchConditionAddReq) {
        SearchConditionDO searchConditionDO = SearchConditionCopier.INSTANCE.convert(searchConditionAddReq);
        searchConditionMapper.insert(searchConditionDO);

        return BaseResult.success(true);
    }
}
