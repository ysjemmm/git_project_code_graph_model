package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.client.UseCasePlatFormCallService;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.service.utils.HttpUtil;
import com.timevale.forward.service.utils.JsonUtils;
import com.timevale.forward.service.utils.http.UseCaseQueryConfigUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UseCasePlatFormCallServiceImpl implements UseCasePlatFormCallService {

    @Resource
    private UseCaseQueryConfigUtil queryConfigUtil;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Override
    public BaseResult queryProjectGroupList(Map<String, Object> params) {
        String res = HttpUtil.doGet(queryConfigUtil.getQueryProjectGroupListUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryCasePlatformProjectList(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getQueryCasePlatformProjectListUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult addUseCasePlatformVersion(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getAddUseCasePlatformVersionUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryVersionList(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getQueryVersionListUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryTurnList(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getQueryTurnListUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryDemandCaseList(Map<String, Object> params) {
        BaseResult baseResult = new BaseResult();
        String res = HttpUtil.doPost(queryConfigUtil.getQueryDemandCaseListUrl(), params);
        Map<String, Object> data = (Map<String, Object>) Optional.of(JsonUtils.fromJson(res, BaseResult.class)).map(BaseResult::getData).get();
        if (MapUtils.isNotEmpty(data)) {
            List<Map<String, Object>> demandCaseList = (List<Map<String, Object>>) data.get("data");
            if (MapUtils.isNotEmpty(params) && StringUtils.isNotEmpty(MapUtils.getString(params, "demandName"))) {
                List<Long> demandIds = demandCaseList.stream().map(demandCase -> Long.valueOf(String.valueOf(demandCase.get("demandId")))).collect(Collectors.toList());

                List<ProductDemandDO> productDemandDOS = productDemandMapper.selectByIdList(demandIds);
                Map<Long, ProductDemandDO> demandDOMap = productDemandDOS.stream().collect(Collectors.toMap(ProductDemandDO::getId, Function.identity()));

                for (Map<String, Object> demandCaseMap : demandCaseList) {
                    Long demandId = Long.valueOf(String.valueOf(demandCaseMap.get("demandId")));
                    ProductDemandDO productDemandDO = demandDOMap.get(demandId);
                    if (Objects.nonNull(productDemandDO)) {
                        demandCaseMap.put("demandName", productDemandDO.getName());
                        demandCaseMap.put("desc", productDemandDO.getDesc());
                        demandCaseMap.put("priorityName", PriorityEnum.getTextByCode(productDemandDO.getPriority()));
                        demandCaseMap.put("ownerId", productDemandDO.getOwnerId());
                        demandCaseMap.put("ownerName", productDemandDO.getOwner());
                    }
                }

                data.put("data", demandCaseList);
            }
        }

        baseResult.setData(data);
        return baseResult;
    }

    @Override
    public BaseResult queryCaseList(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getQueryCaseListUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryVersionLinkCaseCount(Map<String, Object> params) {
        String res = HttpUtil.doGet(queryConfigUtil.getQueryVersionLinkCaseCountUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryTurnTreeList(Map<String, Object> params) {
        String res = HttpUtil.doGet(queryConfigUtil.getQueryTurnTreeListUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }
}
