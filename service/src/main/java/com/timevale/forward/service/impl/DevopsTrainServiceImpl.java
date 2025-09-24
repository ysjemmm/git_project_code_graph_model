package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.DevopsProjectTrainRelMapper;
import com.timevale.forward.dal.entity.DevopsProjectTrainRelDO;
import com.timevale.forward.facade.api.client.DevopsTrainService;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.enums.BaseResultCodeEnum;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Devops发布火车服务实现
 *
 * @author xingyun
 * @date 2025/9/17
 */
@Slf4j
@LogPoint
@RestService
public class DevopsTrainServiceImpl implements DevopsTrainService {

    @Resource
    private PublishPlatformClient platformClient;

    @Resource
    private DevopsProjectTrainRelMapper projectPublishTrainRelMapper;

    /**
     * 获取发布火车列表
     *
     * @param params 查询参数
     * @return 发布火车列表数据
     */
    @Override
    public BaseResult<PageQueryResult<Map<String, Object>>> getTrainList(Map<String, Object> params) {
        try {
            Map<String, Object> result = platformClient.getTrainList(params);
            if (result == null) {
                return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车列表失败");
            }

            // 解析分页数据
            PageQueryResult<Map<String, Object>> pageResult = new PageQueryResult<>();
            pageResult.setResultList((java.util.List<Map<String, Object>>) result.get("list"));
            pageResult.setTotalItems((Integer) result.get("count"));

            return BaseResult.success(pageResult);
        } catch (Exception e) {
            log.error("获取发布火车列表异常", e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车列表异常: " + e.getMessage());
        }
    }

    /**
     * 获取发布火车详情
     * 获取发布火车基础信息
     * @param trainId 发布火车ID
     * @return 发布火车基础信息
     */
    @Override
    public BaseResult<Map<String, Object>> getTrainDetail(Integer trainId) {
        try {
            Map<String, Object> result = platformClient.getTrainDetail(trainId);
            if (result == null) {
                return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车详情失败");
            }

            // 直接返回结果数据
            return BaseResult.success(result);
        } catch (Exception e) {
            log.error("获取发布火车详情异常, trainId: {}", trainId, e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车详情异常: " + e.getMessage());
        }
    }

    /**
     * 关联一个批量发布
     * @param trainId 批量发布Id
     * @param projectId 产研项目Id
     * @return {@link BaseResult}<{@link Boolean}>
     */

    @Override
    public BaseResult<Boolean> linkProjectToTrain(Integer trainId, Integer projectId) {
        try {
            // 1. 参数校验
            if (trainId == null || projectId == null) {
                return BaseResult.fail(BaseResultCodeEnum.ILLEGAL_ARGUMENT.getNCode(), "参数不能为空");
            }

            // 2. 检查关联关系是否已存在
            int count = projectPublishTrainRelMapper.countRelation(projectId, trainId);
            if (count > 0) {
                return BaseResult.fail(BaseResultCodeEnum.DATA_ERROR.getNCode(), "该项目已关联此批量发布");
            }

            // 3. 检查批量发布是否存在
            Map<String, Object> params = new HashMap<>();
            params.put("ids", Collections.singletonList(trainId));

            BaseResult<PageQueryResult<Map<String, Object>>> trainListResult = getTrainList(params);
            if (!trainListResult.ifSuccess() || trainListResult.getData() == null) {
                return BaseResult.fail(BaseResultCodeEnum.DATA_ERROR.getNCode(), "指定的批量发布不存在");
            }

            // 4. 获取当前操作人信息
            UserInfo userInfo = LocalSessionUtils.getUserInfo();

            // 5. 新增关联关系
            projectPublishTrainRelMapper.insertRelation(projectId, trainId, userInfo.getAlias(), userInfo.getId());

            log.info("关联项目到批量发布成功, projectId={}, trainId={}, operator={}", projectId, trainId, userInfo.getAlias());
            return BaseResult.success(true);
        } catch (Exception e) {
            log.error("关联项目到批量发布失败, projectId={}, trainId={}", projectId, trainId, e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "关联失败: " + e.getMessage());
        }
    }

    /**
     * 取消关联一个批量发布
     * @param trainId 批量发布Id
     * @param projectId 产研项目Id
     * @return {@link BaseResult}<{@link Boolean}>
     */
    @Override
    public BaseResult<Boolean> unlinkProjectFromTrain(Integer trainId, Integer projectId) {
        try {
            // 1. 参数校验
            // 1. 参数校验
            if (trainId == null || projectId == null) {
                return BaseResult.fail(BaseResultCodeEnum.ILLEGAL_ARGUMENT.getNCode(), "参数不能为空");
            }

            // 2. 检查关联关系是否已存在
//            int count = projectPublishTrainRelMapper.countRelation(projectId, trainId);
//            if (count > 0) {
//                return BaseResult.fail(BaseResultCodeEnum.DATA_ERROR.getNCode(), "关联关系不存在");
//            }

            // 3. 获取当前操作人信息
            UserInfo userInfo = LocalSessionUtils.getUserInfo();

            // 4. 软删除关联关系
            projectPublishTrainRelMapper.deleteRelation(projectId, trainId, userInfo.getAlias(), userInfo.getId());

            log.info("取消关联成功");
            return BaseResult.success(true);
        } catch (Exception e) {
            log.error("取消关联失败, e");
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "取消关联失败: " + e.getMessage());
        }
    }

    @Override
    public BaseResult<PageQueryResult<Map<String, Object>>> getTrainListByProjectId(
            Integer projectId,
            Integer page,
            Integer pageSize) {

        try {
            // 1. 参数校验
            if (projectId == null) {
                return BaseResult.fail(400, "项目ID不能为空");
            }
            if (page == null || page < 1) {
                page = 1; // 默认第一页
            }
            if (pageSize == null || pageSize < 1) {
                pageSize = 10; // 默认每页10条
            }

            // 2. 使用 PageHelper 开始分页（必须紧邻第一个查询语句）
            PageHelper.startPage(page, pageSize);

            // 3. 执行分页查询 - 修改为获取完整的关联信息（包含关联表ID）
            List<DevopsProjectTrainRelDO> relationInfoList = projectPublishTrainRelMapper.selectTrainRelationsByProjectId(projectId);

            // 4. 包装分页信息
            PageInfo<DevopsProjectTrainRelDO> pageInfo = new PageInfo<>(relationInfoList);

            // 5. 如果没有关联的批量发布，直接返回空结果
            if (pageInfo.getList().isEmpty()) {
                PageQueryResult<Map<String, Object>> emptyResult = new PageQueryResult<>();
                emptyResult.setResultList(Collections.emptyList());
                emptyResult.setTotalItems((int) pageInfo.getTotal());
                log.info("项目没有关联的批量发布, projectId={}", projectId);
                return BaseResult.success(emptyResult);
            }

            // 6. 提取trainId列表并保持与关联表ID的映射关系
            List<Integer> trainIds = pageInfo.getList().stream()
                    .map(DevopsProjectTrainRelDO::getPublishTrainId)
                    .collect(Collectors.toList());

            // 7. 构建查询参数（固定分页为第一页）
            Map<String, Object> params = new HashMap<>();
            params.put("ids", trainIds);
            params.put("page", 1); // 固定第一页
            params.put("pageSize", trainIds.size()); // 请求全部数据

            // 8. 调用平台接口获取数据
            BaseResult<PageQueryResult<Map<String, Object>>> trainListResult = getTrainList(params);

            // 9. 处理平台接口返回结果
            if (!trainListResult.ifSuccess() || trainListResult.getData() == null) {
                log.error("获取批量发布列表失败, projectId={}, msg={}",
                        projectId, trainListResult.getMessage());
                return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取批量发布列表失败");
            }

            // 10. 重组分页结果并按关联表ID排序
            PageQueryResult<Map<String, Object>> remoteResult = trainListResult.getData();
            List<Map<String, Object>> trainList = remoteResult.getResultList();

            // 创建trainId到train数据的映射
            Map<Integer, Map<String, Object>> trainDataMap = trainList.stream()
                    .collect(Collectors.toMap(
                            train -> (Integer) train.get("id"),
                            train -> train
                    ));

            // 按照关联表ID顺序重新排列结果
            List<Map<String, Object>> sortedTrainList = pageInfo.getList().stream()
                    .map(relation -> {
                        Integer trainId = relation.getPublishTrainId();
                        Map<String, Object> trainData = trainDataMap.get(trainId);
                        if (trainData != null) {
                            // 可以选择性地添加关联表信息到结果中
                            Map<String, Object> result = new HashMap<>(trainData);
                            result.put("relationId", relation.getId()); // 添加关联表ID
                            result.put("relationCreateDate", relation.getCreateDate()); // 添加关联创建时间
                            return result;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            PageQueryResult<Map<String, Object>> pageResult = new PageQueryResult<>();
            pageResult.setResultList(sortedTrainList);
            pageResult.setTotalItems((int) pageInfo.getTotal()); // 使用 PageHelper 的总记录数

            log.info("获取项目批量发布列表成功, projectId={}, page={}, pageSize={}, total={}",
                    projectId, page, pageSize, pageResult.getTotalItems());
            return BaseResult.success(pageResult);

        } catch (Exception e) {
            log.error("获取项目批量发布列表失败, projectId={}", projectId, e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取列表失败: " + e.getMessage());
        } finally {
            // 清除 PageHelper 的分页参数，避免影响其他查询
            PageHelper.clearPage();
        }
    }

    /**
     * 获取发布火车-发布详情
     * @param trainId 发布火车ID
     * @param params 查询参数
     * @return 发布火车-发布详情
     */
    public BaseResult<Map<String, Object>> getPublishPage(Integer trainId, Map<String, Object> params) {
        try {
            Map<String, Object> result = platformClient.getTrainPublishPage(trainId, params);

            if (result == null || result.isEmpty()) {
                return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车-应用发布详情信息失败");
            }

            return BaseResult.success(result);

        } catch (Exception e) {
            log.error("获取发布火车详情异常, trainId: {}, params: {}", trainId, params, e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取发布火车-应用发布详情信息异常: " + e.getMessage());
        }
    }

    /**
     * 根据批量发布ID获取产研项目ID列表
     *
     * @param trainId 发布火车ID
     * @return 产研项目ID列表
     */
    public BaseResult<List<Long>> getProjectIdsByTrainId(Integer trainId) {
        try {
            // 参数校验
            if (trainId == null) {
                return BaseResult.fail(BaseResultCodeEnum.NULL_ARGUMENT.getNCode(), "批量发布ID不能为空");
            }

            // 查询关联的项目ID列表
            List<Long> projectIds = projectPublishTrainRelMapper.selectProjectIdsByTrainId(trainId);

            // 直接返回结果，为空也正常返回
            if (projectIds == null) {
                projectIds = new ArrayList<>();
            }

            log.info("获取产研项目ID列表, trainId: {}, 项目数量: {}", trainId, projectIds.size());
            return BaseResult.success(projectIds);

        } catch (Exception e) {
            log.error("根据批量发布ID获取产研项目ID列表异常, trainId: {}", trainId, e);
            return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(),
                    "获取产研项目ID列表异常: " + e.getMessage());
        }
    }
}
