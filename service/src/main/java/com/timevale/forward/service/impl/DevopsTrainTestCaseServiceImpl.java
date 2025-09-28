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
public class DevopsTrainTestCaseServiceImpl implements DevopsTrainTestCaseService {
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

            // 3. 获取所有轮次数据
            List<Map<String, Object>> allTurns = getAllTurnsByProjectIds(projectIds);

            // 检查是否收集到轮次数据
            if (CollectionUtils.isEmpty(allTurns)) {
                log.info("所有项目均未找到轮次信息, trainId: {}", trainId);
                return BaseResult.success(createEmptyResult());
            }

            // 4. 构建四级级联结构：groupName -> projectName -> versionName -> turnName
            List<Map<String, Object>> cascadeData = buildFourLevelCascade(allTurns);

            // 5. 组装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("cascadeData", cascadeData);

            log.info("获取发布火车测试用例级联信息成功, trainId: {}, projectCount: {}, turnCount: {}",
                    trainId, projectIds.size(), allTurns.size());

            return BaseResult.success(result);

        } catch (Exception e) {
            log.error("根据发布火车ID获取测试用例级联信息异常, trainId: {}", trainId, e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(),
                    "获取测试用例级联信息异常: " + e.getMessage());
        }
    }

    /**
     * 根据产研项目ID列表获取所有轮次数据
     *
     * @param projectIds 产研项目ID列表
     * @return 所有轮次数据列表
     */
    private List<Map<String, Object>> getAllTurnsByProjectIds(List<Long> projectIds) {
        List<Map<String, Object>> allTurns = new ArrayList<>();

        for (Long projectId : projectIds) {
            try {
                // 获取项目下的版本列表
                List<Map<String, Object>> versions = getVersionsByProjectId(projectId);

                if (CollectionUtils.isNotEmpty(versions)) {
                    for (Map<String, Object> version : versions) {
                        Object versionIdObj = version.get("id");
                        String versionName = (String) version.get("versionName");

                        if (versionIdObj != null && versionName != null) {
                            Integer versionId = parseVersionId(versionIdObj);
                            if (versionId != null) {
                                // 获取该版本下的轮次数据
                                List<Map<String, Object>> turns = getTurnsByVersionId(versionId);

                                // 为每个轮次添加版本信息
                                for (Map<String, Object> turn : turns) {
                                    turn.put("versionName", versionName);
                                    turn.put("chanyanProjectId", projectId);
                                    allTurns.add(turn);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("获取项目轮次数据异常, projectId: {}", projectId, e);
            }
        }

        return allTurns;
    }

    /**
     * 根据项目ID获取版本列表
     *
     * @param projectId 项目ID
     * @return 版本列表
     */
    private List<Map<String, Object>> getVersionsByProjectId(Long projectId) {
        List<Map<String, Object>> versions = new ArrayList<>();

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("chanyanProjectId", projectId);
            params.put("pageIndex", 1);
            params.put("pageSize", 1000);

            BaseResult versionResult = useCasePlatFormCallService.queryVersionList(params);
            if (versionResult.ifSuccess() && versionResult.getData() != null) {
                Map<String, Object> data = (Map<String, Object>) versionResult.getData();
                List<Map<String, Object>> versionList = (List<Map<String, Object>>) data.get("tmsVersionVOs");

                if (CollectionUtils.isNotEmpty(versionList)) {
                    versions = versionList;
                }
            }
        } catch (Exception e) {
            log.error("获取项目版本列表异常, projectId: {}", projectId, e);
        }

        return versions;
    }

    /**
     * 根据版本ID获取轮次列表
     *
     * @param versionId 版本ID
     * @return 轮次列表
     */
    private List<Map<String, Object>> getTurnsByVersionId(Integer versionId) {
        List<Map<String, Object>> turns = new ArrayList<>();

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("versionId", versionId);
            params.put("pageIndex", 1);
            params.put("pageSize", 1000);

            BaseResult turnResult = useCasePlatFormCallService.queryTurnList(params);
            if (turnResult.ifSuccess() && turnResult.getData() != null) {
                Object data = turnResult.getData();

                // 处理分页结构的数据
                if (data instanceof Map) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    Object turnListData = dataMap.get("tmsTurnVOs");
                    if (turnListData instanceof List) {
                        turns = (List<Map<String, Object>>) turnListData;
                    }
                } else if (data instanceof List) {
                    turns = (List<Map<String, Object>>) data;
                }
            }
        } catch (Exception e) {
            log.error("获取版本轮次列表异常, versionId: {}", versionId, e);
        }

        return turns;
    }

    /**
     * 构建四级级联结构
     *
     * @param allTurns 所有轮次数据
     * @return 四级级联结构数据
     */
    private List<Map<String, Object>> buildFourLevelCascade(List<Map<String, Object>> allTurns) {
        List<Map<String, Object>> cascadeData = new ArrayList<>();

        // 第一步：按 groupName 分组
        Map<String, List<Map<String, Object>>> groupMap = new LinkedHashMap<>();
        for (Map<String, Object> turn : allTurns) {
            String groupName = (String) turn.get("groupName");
            if (groupName != null) {
                groupMap.computeIfAbsent(groupName, k -> new ArrayList<>()).add(turn);
            }
        }

        // 第二步：构建四级结构
        for (Map.Entry<String, List<Map<String, Object>>> groupEntry : groupMap.entrySet()) {
            String groupName = groupEntry.getKey();
            List<Map<String, Object>> groupTurns = groupEntry.getValue();

            // 构建组节点（第一级）
            Map<String, Object> groupNode = new HashMap<>();
            groupNode.put("id", getGroupId(groupTurns)); // 从轮次数据中获取groupId
            groupNode.put("name", groupName);
            groupNode.put("type", "group");

            // 按项目名称分组
            Map<String, List<Map<String, Object>>> projectMap = new LinkedHashMap<>();
            for (Map<String, Object> turn : groupTurns) {
                String projectName = (String) turn.get("projectName");
                if (projectName != null) {
                    projectMap.computeIfAbsent(projectName, k -> new ArrayList<>()).add(turn);
                }
            }

            // 构建项目节点列表（第二级）
            List<Map<String, Object>> projectNodes = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> projectEntry : projectMap.entrySet()) {
                String projectName = projectEntry.getKey();
                List<Map<String, Object>> projectTurns = projectEntry.getValue();

                // 构建项目节点
                Map<String, Object> projectNode = new HashMap<>();
                projectNode.put("id", getProjectId(projectTurns)); // 从轮次数据中获取projectId
                projectNode.put("name", projectName);
                projectNode.put("type", "project");

                // 按版本名称分组
                Map<String, List<Map<String, Object>>> versionMap = new LinkedHashMap<>();
                for (Map<String, Object> turn : projectTurns) {
                    String versionName = (String) turn.get("versionName");
                    if (versionName != null) {
                        versionMap.computeIfAbsent(versionName, k -> new ArrayList<>()).add(turn);
                    }
                }

                // 构建版本节点列表（第三级）
                List<Map<String, Object>> versionNodes = new ArrayList<>();
                for (Map.Entry<String, List<Map<String, Object>>> versionEntry : versionMap.entrySet()) {
                    String versionName = versionEntry.getKey();
                    List<Map<String, Object>> versionTurns = versionEntry.getValue();

                    // 构建版本节点
                    Map<String, Object> versionNode = new HashMap<>();
                    versionNode.put("id", getVersionId(versionTurns)); // 从轮次数据中获取versionId
                    versionNode.put("name", versionName);
                    versionNode.put("type", "version");

                    // 构建轮次节点列表（第四级）
                    List<Map<String, Object>> turnNodes = new ArrayList<>();
                    for (Map<String, Object> turn : versionTurns) {
                        Map<String, Object> turnNode = new HashMap<>();
                        turnNode.put("id", turn.get("id")); // 轮次的真实ID
                        turnNode.put("name", turn.get("turnName")); // 轮次名称
                        turnNode.put("type", "turn");

                        // 保留轮次的所有原始数据
                        turnNode.putAll(turn);

                        turnNodes.add(turnNode);
                    }

                    versionNode.put("children", turnNodes);
                    versionNodes.add(versionNode);
                }

                projectNode.put("children", versionNodes);
                projectNodes.add(projectNode);
            }

            groupNode.put("children", projectNodes);
            cascadeData.add(groupNode);
        }

        return cascadeData;
    }

    /**
     * 从轮次数据中获取组ID
     */
    private Object getGroupId(List<Map<String, Object>> turns) {
        if (CollectionUtils.isNotEmpty(turns)) {
            return turns.get(0).get("groupId");
        }
        return null;
    }

    /**
     * 从轮次数据中获取项目ID
     */
    private Object getProjectId(List<Map<String, Object>> turns) {
        if (CollectionUtils.isNotEmpty(turns)) {
            return turns.get(0).get("projectId");
        }
        return null;
    }

    /**
     * 从轮次数据中获取版本ID
     */
    private Object getVersionId(List<Map<String, Object>> turns) {
        if (CollectionUtils.isNotEmpty(turns)) {
            return turns.get(0).get("versionId");
        }
        return null;
    }

    /**
     * 解析版本ID
     */
    private Integer parseVersionId(Object versionIdObj) {
        if (versionIdObj == null) {
            return null;
        }

        if (versionIdObj instanceof Integer) {
            return (Integer) versionIdObj;
        } else if (versionIdObj instanceof Long) {
            return ((Long) versionIdObj).intValue();
        } else if (versionIdObj instanceof String) {
            try {
                return Integer.valueOf((String) versionIdObj);
            } catch (NumberFormatException e) {
                log.warn("无法解析版本ID: {}", versionIdObj);
                return null;
            }
        }

        return null;
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
