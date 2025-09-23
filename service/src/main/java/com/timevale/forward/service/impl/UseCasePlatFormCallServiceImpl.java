package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.client.UseCasePlatFormCallService;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.service.utils.HttpUtil;
import com.timevale.forward.service.utils.JsonUtils;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.http.UseCaseQueryConfigUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@LogPoint
@RestService
public class UseCasePlatFormCallServiceImpl implements UseCasePlatFormCallService {

    @Resource
    private UseCaseQueryConfigUtil queryConfigUtil;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProjectService projectService;

    @Override
    public BaseResult queryProjectGroupList(Map<String, Object> params) {
        BaseResult baseResult = new BaseResult();
        String res = HttpUtil.doGet(queryConfigUtil.getQueryProjectGroupListUrl(), params);
        List<Map<String, Object>> groupList;
        try {
            groupList = (List<Map<String, Object>>) Optional.of(JsonUtils.fromJson(res, BaseResult.class)).map(BaseResult::getData).get();
        } catch (Exception e) {
            throw new BaseBizRuntimeException("获取用例平台业务组信息错误");
        }
        if (CollectionUtils.isNotEmpty(groupList) && MapUtils.isNotEmpty(params)) {
            Long projectId = MapUtils.getLong(params, "chanyanProjectId");
            ProjectDetailVO projectDetailVO = Optional.of(projectService.get(projectId)).map(BaseResult::getData).get();
            if (projectDetailVO == null) {
                throw new BaseBizRuntimeException("项目不存在");
            }
            List<Long> bizDomainIds = projectDetailVO.getProductLineVO().stream().map(ProductLineVO::getBizDomainId).collect(Collectors.toList());
            // 过滤出符合的业务线的业务组
            groupList = groupList.stream().filter(group -> bizDomainIds.contains(MapUtils.getLong(group, "bizDomainId"))).collect(Collectors.toList());
        }
        baseResult.setData(filterData(groupList));
        return baseResult;
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
        List<Map<String, Object>> demandCaseList;
        try {
            demandCaseList = (List<Map<String, Object>>) Optional.of(JsonUtils.fromJson(res, BaseResult.class)).map(BaseResult::getData).get();
        } catch (Exception e) {
            throw new BaseBizRuntimeException("获取用例平台需求用例信息错误");
        }
        if (CollectionUtils.isNotEmpty(demandCaseList)) {
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

            if (MapUtils.isNotEmpty(params) && StringUtils.isNotEmpty(MapUtils.getString(params, "demandName"))) {
                String demandName = MapUtils.getString(params, "demandName");
                demandCaseList = demandCaseList.stream().filter(demandCase -> demandName.equals(demandCase.get("demandName"))).collect(Collectors.toList());
            }

            if (MapUtils.isNotEmpty(params) && StringUtils.isNotEmpty(MapUtils.getString(params, "testResult"))) {
                String testResult = MapUtils.getString(params, "testResult");
                demandCaseList = demandCaseList.stream().filter(demandCase -> testResult.equals(demandCase.get("testResult"))).collect(Collectors.toList());
            }
            baseResult.setData(demandCaseList);
        }

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
        // 从字符串res中获取到"demandId": null的集合，然后获取到demandId不为空的集合，然后返回
        BaseResult baseResult = JsonUtils.fromJson(res, BaseResult.class);
        if (baseResult != null && baseResult.getData() != null) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) baseResult.getData();
            List<Map<String, Object>> filteredData = filterData(data);
            baseResult.setData(filteredData);
        }

        return baseResult;
    }

    private List<Map<String, Object>> filterData(List<Map<String, Object>> data) {
        return data.stream().map(this::filterModule).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Map<String, Object> filterModule(Map<String, Object> module) {
        if (module == null) return null;

        Map<String, Object> result = new HashMap<>(module);

        if (module.containsKey("tmsTreeVO") && module.get("tmsTreeVO") != null) {
            Map<String, Object> tmsTree = (Map<String, Object>) module.get("tmsTreeVO");
            Map<String, Object> filteredTmsTree = new HashMap<>(tmsTree);

            // 过滤caseList中demandId不为null的项
            if (tmsTree.containsKey("caseList") && tmsTree.get("caseList") != null) {
                List<Map<String, Object>> caseList = (List<Map<String, Object>>) tmsTree.get("caseList");
                List<Long> demandIds = caseList.stream()
                        .filter(caseItem -> caseItem.get("demandId") != null)
                        .map(caseItem -> Long.valueOf(String.valueOf(caseItem.get("demandId"))))
                        .collect(Collectors.toList());
                Map<Long, String> demandNameMap = new HashMap<>();
                if (CollectionUtils.isNotEmpty(demandIds)) {
                    demandNameMap = productDemandMapper.selectByIdList(demandIds).stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getName, (oldValue, newValue) -> oldValue));
                }
                for (Map<String, Object> caseItem : caseList) {
                    if (caseItem.get("demandId") == null){
                        caseItem.put("demandName", null);
                    } else {
                        caseItem.put("demandName", demandNameMap.get(Long.valueOf(String.valueOf(caseItem.get("demandId")))));
                    }
                }
                filteredTmsTree.put("caseList", caseList);
            }

            // 递归处理moduleList
            if (tmsTree.containsKey("moduleList") && tmsTree.get("moduleList") != null) {
                List<Map<String, Object>> moduleList = (List<Map<String, Object>>) tmsTree.get("moduleList");
                filteredTmsTree.put("moduleList", filterData(moduleList));
            }

            result.put("tmsTreeVO", filteredTmsTree);
        }

        return result;
    }

    @Override
    public BaseResult queryTurnProgress(Map<String, Object> params) {
        String res = HttpUtil.doGet(queryConfigUtil.getQueryTurnProgressUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryCase(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getQueryCaseUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult editCase(Map<String, Object> params) {
        String res = HttpUtil.doPut(queryConfigUtil.getEditCaseUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult deleteCase(Map<String, Object> params) {
        String res = HttpUtil.doPut(queryConfigUtil.getDeleteCaseUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult linkOrUnLinkDemand(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getLinkOrUnLinkDemandUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult signCaseResult(Map<String, Object> params) {
        String res = HttpUtil.doPut(queryConfigUtil.getSignCaseResultUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult addTurn(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getAddTurnUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult deleteTurn(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getDeleteTurnUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryTurnName(Map<String, Object> params) {
        String res = HttpUtil.doGet(queryConfigUtil.getQueryTurnNameUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryGroupProjectVersionList(Map<String, Object> params) {
        String res = HttpUtil.doGet(queryConfigUtil.queryGroupProjectVersionListUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult checkCaseOverTime(Map<String, Object> params) {
        String res = HttpUtil.doGet(queryConfigUtil.queryCheckCaseOverTimeUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryCaseOverTime(Map<String, Object> params) {
        String res = HttpUtil.doGet(queryConfigUtil.queryQueryCaseOverTimeUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult caseImageUpload(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.caseImageUploadUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult caseDocumentDelete(Map<String, Object> params) {
        String res = HttpUtil.doDelete(queryConfigUtil.caseDocumentDeleteUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult addCase(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.addCaseUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }
}
