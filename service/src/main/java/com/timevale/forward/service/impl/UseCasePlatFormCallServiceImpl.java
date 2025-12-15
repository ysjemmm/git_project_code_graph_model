package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.UseCasePlatFormCallService;
import com.timevale.forward.service.component.ProductLineComponent;
import com.timevale.forward.service.utils.HttpUtil;
import com.timevale.forward.service.utils.JsonUtils;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.http.UseCaseQueryConfigUtil;
import com.timevale.mandarin.base.enums.BaseResultCodeEnum;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@LogPoint
@RestService
public class UseCasePlatFormCallServiceImpl implements UseCasePlatFormCallService {

    @Resource
    private UseCaseQueryConfigUtil queryConfigUtil;

    @Resource
    private ProductLineComponent productLineComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Override
    public BaseResult addTestPlanModule(Map<String, Object> params) {
        Long devProjectId = MapUtils.getLong(params, "devProjectId");
        Integer testTurnType = MapUtils.getInteger(params, "testTurnType");
        if (devProjectId == null) {
            return BaseResult.fail(BaseResultCodeEnum.DATA_ERROR.getNCode(),"项目不存在");
        }
        ProjectDO projectDO = projectMapper.get(devProjectId);
        // 获取产品线
        Long productLineId = MapUtils.getLong(params, "productLineId");
        String planName = MapUtils.getString(params, "planName");
        if (productLineId == null) {
            return BaseResult.fail(BaseResultCodeEnum.DATA_ERROR.getNCode(),"产品线不存在");
        }
        if (StringUtils.isEmpty(planName)) {
            return BaseResult.fail(BaseResultCodeEnum.DATA_ERROR.getNCode(),"测试计划名称不能为空");
        }
        // 根据产品线获取到业务域
        ProductLineDO productLineDO = productLineComponent.getById(productLineId);
        Map<String, Object> queryProjectParams = new HashMap<>();
        queryProjectParams.put("projectNum", productLineDO.getBizDomainId());
        String projectRes = HttpUtil.doGet(queryConfigUtil.getQueryProjectUrl(), queryProjectParams);
        Map<String, Object> project;
        try {
            project = (Map<String, Object>) Optional.of(JsonUtils.fromJson(projectRes, BaseResult.class)).map(BaseResult::getData).get();
        } catch (Exception e) {
            throw new BaseBizRuntimeException("获取用例平台项目信息失败");
        }
        if (Objects.nonNull(project)) {
            Map<String, Object> addTestPlanModuleParams = new HashMap<>();
            String projectId = MapUtils.getString(project, "id");
            addTestPlanModuleParams.put("projectId", projectId);
            addTestPlanModuleParams.put("name", projectDO.getName());
            addTestPlanModuleParams.put("parentId", "NONE");
            addTestPlanModuleParams.put("devProjectId", projectDO.getId());
            // 检查是否已经存在模块
            Boolean isExistModule = (Boolean) Optional.of(checkModuleExist(addTestPlanModuleParams)).map(BaseResult::getData).get();
            if (isExistModule != null && isExistModule) {
                return addTestPlanAndGetResult(projectId, projectDO, planName, testTurnType);
            } else {
                String addModuleRes = HttpUtil.doPost(queryConfigUtil.getAddTestPlanModuleUrl(), addTestPlanModuleParams);
                BaseResult baseResult = JsonUtils.fromJson(addModuleRes, BaseResult.class);
                if (baseResult != null && baseResult.getData() != null) {
                    return addTestPlanAndGetResult(projectId, projectDO, planName, testTurnType);
                }
            }
        }

        return BaseResult.success();
    }

    private BaseResult addTestPlanAndGetResult(String projectId, ProjectDO projectDO, String planName, Integer testTurnType) {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", projectId);
        map.put("moduleId", projectDO.getId());
        map.put("type", "TEST_PLAN");
        map.put("name", planName);
        map.put("planType", testTurnType);
        // 添加测试计划
        return addTestPlan(map);
    }

    @Override
    public BaseResult addTestPlan(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getAddTestPlanUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult queryProject(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getQueryProjectUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult statistics(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getStatisticsUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult searchProjects(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getSearchProjectsUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult testplanDetails(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getTestplanDetailsUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult checkModuleExist(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getCheckModuleExistUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }
}
