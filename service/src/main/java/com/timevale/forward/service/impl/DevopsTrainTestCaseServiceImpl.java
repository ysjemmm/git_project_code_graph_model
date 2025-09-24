package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.DevopsTrainService;
import com.timevale.forward.facade.api.client.UseCasePlatFormCallService;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.enums.BaseResultCodeEnum;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 发布火车测试用例服务实现
 *
 * @author dijiu
 * @date 2025/09/24
 */
@Slf4j
@LogPoint
@RestService
public class DevopsTrainTestCaseServiceImpl {

    @Resource
    private DevopsTrainService devopsTrainService;

    @Resource
    private UseCasePlatFormCallService useCasePlatFormCallService;

    /**
     * 根据发布火车ID获取测试用例版本和轮次级联信息
     *
     * @param trainId 发布火车ID
     * @return 测试用例版本和轮次级联信息
     */
    public BaseResult<Map<String, Object>> getTestCaseCascadeInfoByTrainId(Integer trainId) {
        try {
            // 1. 参数校验
            if (trainId == null) {
                return BaseResult.fail(BaseResultCodeEnum.NULL_ARGUMENT.getNCode(), "发布火车ID不能为空");
            }

            // 2. 根据发布火车ID获取产研项目ID列表
            BaseResult<List<Long>> projectIdsResult = devopsTrainService.getProjectIdsByTrainId(trainId);
            if (!projectIdsResult.ifSuccess()) {
                log.error("根据发布火车ID获取产研项目ID列表失败, trainId: {}, error: {}",
                        trainId, projectIdsResult.getMessage());
                return BaseResult.fail(projectIdsResult.getCode(), projectIdsResult.getMessage());
            }

            List<Long> projectIds = projectIdsResult.getData();
            if (CollectionUtils.isEmpty(projectIds)) {
                log.info("发布火车未关联任何产研项目, trainId: {}", trainId);
                return BaseResult.success(createEmptyResult());
            }

            log.info("发布火车关联的产研项目ID列表, trainId: {}, projectIds: {}", trainId, projectIds);

            // 3. 获取测试用例版本列表
            List<Map<String, Object>> versionList = getVersionListByProjectIds(projectIds);

            // 4. 构建版本-轮次级联数据
            List<Map<String, Object>> cascadeData = buildVersionTurnCascadeData(versionList, projectIds);

            // 5. 组装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("cascadeData", cascadeData);

            log.info("获取发布火车测试用例级联信息成功, trainId: {}, projectCount: {}, versionCount: {}",
                    trainId, projectIds.size(), versionList.size());

            return BaseResult.success(result);

        } catch (Exception e) {
            log.error("根据发布火车ID获取测试用例级联信息异常, trainId: {}", trainId, e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(),
                    "获取测试用例级联信息异常: " + e.getMessage());
        }
    }

    /**
     * 根据产研项目ID列表获取版本列表
     *
     * @param projectIds 产研项目ID列表
     * @return 版本列表
     */
    private List<Map<String, Object>> getVersionListByProjectIds(List<Long> projectIds) {
        List<Map<String, Object>> allVersions = new ArrayList<>();

        for (Long projectId : projectIds) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("chanyanProjectId", projectId);
                params.put("pageIndex", 1);
                params.put("pageSize", 1000); // 获取足够多的数据

                BaseResult versionResult = useCasePlatFormCallService.queryVersionList(params);
                if (versionResult.ifSuccess() && versionResult.getData() != null) {
                    Map<String, Object> data = (Map<String, Object>) versionResult.getData();
                    List<Map<String, Object>> versions = (List<Map<String, Object>>) data.get("tmsVersionVOs");

                    if (CollectionUtils.isNotEmpty(versions)) {
                        // 为每个版本添加项目ID信息
                        versions.forEach(version -> version.put("chanyanProjectId", projectId));
                        allVersions.addAll(versions);

                        log.debug("获取项目版本列表成功, projectId: {}, versionCount: {}", projectId, versions.size());
                    }
                } else {
                    log.warn("获取项目版本列表失败, projectId: {}, error: {}", projectId,
                            versionResult != null ? versionResult.getMessage() : "未知错误");
                }
            } catch (Exception e) {
                log.error("获取项目版本列表异常, projectId: {}", projectId, e);
            }
        }

        // 对版本列表进行去重和排序
        return allVersions.stream()
                .collect(Collectors.toMap(
                        version -> version.get("id") + "_" + version.get("chanyanProjectId"), // 使用版本ID+项目ID作为key去重
                        version -> version,
                        (existing, replacement) -> existing // 保留第一个
                ))
                .values()
                .stream()
                .sorted((v1, v2) -> {
                    // 按项目ID排序，再按版本创建时间排序
                    Long projectId1 = (Long) v1.get("chanyanProjectId");
                    Long projectId2 = (Long) v2.get("chanyanProjectId");
                    int projectCompare = projectId1.compareTo(projectId2);
                    if (projectCompare != 0) {
                        return projectCompare;
                    }
                    // 按版本ID倒序排列（新版本在前）
                    Integer versionId1 = (Integer) v1.get("id");
                    Integer versionId2 = (Integer) v2.get("id");
                    return versionId2.compareTo(versionId1);
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建版本-轮次级联数据
     *
     * @param versionList 版本列表
     * @param projectIds  项目ID列表
     * @return 版本-轮次级联数据
     */
    private List<Map<String, Object>> buildVersionTurnCascadeData(List<Map<String, Object>> versionList, List<Long> projectIds) {
        List<Map<String, Object>> cascadeData = new ArrayList<>();

        for (Map<String, Object> version : versionList) {
            try {
                // 创建级联数据项
                Map<String, Object> cascadeItem = new HashMap<>(version);

                // 获取版本ID和项目ID
                Integer versionId = (Integer) version.get("id");

                // 查询该版本对应的轮次列表
                List<Map<String, Object>> turnList = getTurnListByVersionIdAndProjectId(versionId);

                // 将轮次列表添加到级联数据项中
                cascadeItem.put("turnList", turnList);
                cascadeItem.put("turnCount", turnList.size());
                cascadeData.add(cascadeItem);

            } catch (Exception e) {
                log.error("构建版本轮次级联数据异常, version: {}", version, e);
            }
        }

        return cascadeData;
    }

    /**
     * 根据版本ID和项目ID获取轮次列表
     *
     * @param versionId  版本ID
     * @return 轮次列表
     */
    private List<Map<String, Object>> getTurnListByVersionIdAndProjectId(Integer versionId) {
        List<Map<String, Object>> turnList = new ArrayList<>();

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("versionId", versionId);
            params.put("pageIndex", 1);
            params.put("pageSize", 1000);

            BaseResult turnResult = useCasePlatFormCallService.queryTurnList(params);
            if (turnResult.ifSuccess() && turnResult.getData() != null) {
                Object data = turnResult.getData();
                List<Map<String, Object>> turns = new ArrayList<>();

                // 处理分页结构的数据
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    Object turnListData = dataMap.get("tmsTurnVOs"); // 获取轮次列表
                    if (turnListData instanceof List) {
                        turns = (List<Map<String, Object>>) turnListData;
                    }
                } else if (data instanceof List) {
                    turns = (List<Map<String, Object>>) data;
                }

                turnList = turns;
            } else {
                log.warn("获取版本轮次列表失败, versionId: {}, error: {}", versionId,
                        turnResult.getMessage());
            }
        } catch (Exception e) {
            log.error("获取版本轮次列表异常, versionId: {}, projectId: {}", versionId, e);
        }

        return turnList;
    }

     /**
     * 创建空结果
     *
     * @return 空结果
     */
    private Map<String, Object> createEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("cascadeData", Collections.emptyList());
        return result;
    }
}
