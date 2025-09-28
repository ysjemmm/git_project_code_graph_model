package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.DevopsTrainService;
import com.timevale.forward.facade.api.client.DevopsTrainTestCaseService;
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
public class DevopsTrainTestCaseServiceImpl  implements DevopsTrainTestCaseService {
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

            // 3. 获取测试用例版本列表（内部已包含循环处理逻辑）
            List<Map<String, Object>> versionList = getVersionListByProjectIds(projectIds);

            // 检查是否收集到版本数据
            if (CollectionUtils.isEmpty(versionList)) {
                log.info("所有项目均未找到版本信息, trainId: {}", trainId);
                return BaseResult.success(createEmptyResult());
            }

            // 4. 直接构建级联数据结构
            List<Map<String, Object>> cascadeData = new ArrayList<>();

            // 第一个for循环：找出所有不重复的projectId
            Set<Long> projectIdSet = new HashSet<>();
            List<Map<String, Object>> versions = new ArrayList<>();
            List<Map<String, Object>> turns = new ArrayList<>();

            for (Map<String, Object> item : versionList) {
                String type = (String) item.get("type");
                if ("version".equals(type)) {
                    versions.add(item);
                    Long projectId = (Long) item.get("chanyanProjectId");
                    if (projectId != null) {
                        projectIdSet.add(projectId);
                    }
                } else {
                    turns.add(item);
                }
            }

            // 第二个for循环：为每个项目构建数据
            for (Long projectId : projectIdSet) {
                // 找到该项目下的所有版本
                List<Map<String, Object>> projectVersions = new ArrayList<>();
                for (Map<String, Object> version : versions) {
                    Long versionProjectId = (Long) version.get("chanyanProjectId");
                    if (projectId.equals(versionProjectId)) {
                        projectVersions.add(version);
                    }
                }

                // 第三个for循环：为每个版本找到对应的轮次
                for (Map<String, Object> version : projectVersions) {
                    Map<String, Object> versionNode = new HashMap<>();
                    versionNode.put("id", version.get("id"));
                    versionNode.put("name", version.get("name"));
                    versionNode.put("type", "version");
                    versionNode.put("chanyanProjectId", projectId);

                    // 找该版本下的轮次
                    List<Map<String, Object>> versionTurns = new ArrayList<>();
                    Object versionId = version.get("id");

                    for (Map<String, Object> turn : turns) {
                        Object turnVersionId = turn.get("versionId");
                        if (versionId != null && versionId.equals(turnVersionId)) {
                            versionTurns.add(turn);
                        }
                    }

                    versionNode.put("children", versionTurns);
                    cascadeData.add(versionNode);
                }
            }

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
            Map<String, Object> params = new HashMap<>();
            params.put("chanyanProjectId", projectId);
            params.put("pageIndex", 1);
            params.put("pageSize", 1000); // 获取足够多的数据

            BaseResult versionResult = useCasePlatFormCallService.queryVersionList(params);
            if (versionResult.ifSuccess() && versionResult.getData() != null) {
                Map<String, Object> data = (Map<String, Object>) versionResult.getData();
                List<Map<String, Object>> versions = (List<Map<String, Object>>) data.get("tmsVersionVOs");

                if (CollectionUtils.isNotEmpty(versions)) {
                    for (Map<String, Object> version : versions) {
                        // 设置版本基本信息
                        Map<String, Object> versionNode = new HashMap<>();
                        versionNode.put("chanyanProjectId", projectId);
                        versionNode.put("id", version.get("id"));
                        versionNode.put("name", version.get("versionName")); // 根据实际字段调整
                        versionNode.put("type", "version");

                        // 添加版本节点到结果中
                        allVersions.add(versionNode);

                        // 获取并添加该版本下的轮次信息
                        Object versionIdObj = version.get("id");
                        if (versionIdObj != null) {
                            Integer versionId = null;
                            if (versionIdObj instanceof Integer) {
                                versionId = (Integer) versionIdObj;
                            } else if (versionIdObj instanceof Long) {
                                versionId = ((Long) versionIdObj).intValue();
                            } else if (versionIdObj instanceof String) {
                                try {
                                    versionId = Integer.valueOf((String) versionIdObj);
                                } catch (NumberFormatException e) {
                                    log.warn("无法解析版本ID: {}", versionIdObj);
                                    continue;
                                }
                            }

                            if (versionId != null) {
                                List<Map<String, Object>> turns = buildTurnNodes(versionId);
                                if (CollectionUtils.isNotEmpty(turns)) {
                                    allVersions.addAll(turns);
                                }
                            }
                        }
                    }
                }
            }
        }
        return allVersions;
    }

    /**
     * 根据版本ID和项目ID获取轮次列表
     *
     * @param versionId  版本ID
     * @return 轮次列表
     */
    private List<Map<String, Object>> buildTurnNodes(Integer versionId) {
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
