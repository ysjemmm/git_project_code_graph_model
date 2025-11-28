package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.UseCasePlatFormCallService;
import com.timevale.forward.service.utils.HttpUtil;
import com.timevale.forward.service.utils.JsonUtils;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.http.UseCaseQueryConfigUtil;
import com.timevale.mandarin.common.annotation.RestService;
import org.apache.commons.collections4.MapUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@LogPoint
@RestService
public class UseCasePlatFormCallServiceImpl implements UseCasePlatFormCallService {

    @Resource
    private UseCaseQueryConfigUtil queryConfigUtil;

    @Override
    public BaseResult addTestPlanModule(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getAddTestPlanModuleUrl(), params);

        String projectId = MapUtils.getString(params, "projectId");
        String devProjectId = MapUtils.getString(params, "devProjectId");
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", projectId);
        map.put("moduleId", devProjectId);
        map.put("type", "TEST_PLAN");
        map.put("name", "提测预演");
        // 添加测试计划
        addTestPlan(map);

        map.put("name", "线上验证测试");
        addTestPlan(map);

        map.put("name", "第一轮测试");
        addTestPlan(map);
        return JsonUtils.fromJson(res, BaseResult.class);
    }

    @Override
    public BaseResult addTestPlan(Map<String, Object> params) {
        String res = HttpUtil.doPost(queryConfigUtil.getAddTestPlanUrl(), params);
        return JsonUtils.fromJson(res, BaseResult.class);
    }
}
