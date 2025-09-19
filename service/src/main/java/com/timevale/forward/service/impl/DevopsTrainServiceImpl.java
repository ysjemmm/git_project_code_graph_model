package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.DevopsProjectTrainRelMapper;
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
     *
     * @param trainId 发布火车ID
     * @return 发布火车详情数据
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
     * @param id 关联表主健id
     * @return {@link BaseResult}<{@link Boolean}>
     */
    @Override
    public BaseResult<Boolean> unlinkProjectFromTrain(Integer id) {
        try {
            // 1. 参数校验
            if (id == null ) {
                return BaseResult.fail(BaseResultCodeEnum.ILLEGAL_ARGUMENT.getNCode(), "参数不能为空");
            }

            // 2. 检查关联关系是否存在
            int count = projectPublishTrainRelMapper.countRelation1(id);
            if (count == 0) {
                return BaseResult.fail(400, "关联关系不存在");
            }

            // 3. 获取当前操作人信息
            UserInfo userInfo = LocalSessionUtils.getUserInfo();

            // 4. 软删除关联关系
            projectPublishTrainRelMapper.deleteRelation(id, userInfo.getAlias(), userInfo.getId());

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

            // 3. 执行分页查询
            List<Integer> trainIds = projectPublishTrainRelMapper.selectTrainIdsByProjectId(projectId);

            // 4. 包装分页信息
            PageInfo<Integer> pageInfo = new PageInfo<>(trainIds);

            // 5. 如果没有关联的批量发布，直接返回空结果
            if (pageInfo.getList().isEmpty()) {
                PageQueryResult<Map<String, Object>> emptyResult = new PageQueryResult<>();
                emptyResult.setResultList(Collections.emptyList());
                emptyResult.setTotalItems((int) pageInfo.getTotal());
                log.info("项目没有关联的批量发布, projectId={}", projectId);
                return BaseResult.success(emptyResult);
            }

            // 6. 构建查询参数（固定分页为第一页）
            Map<String, Object> params = new HashMap<>();
            params.put("ids", pageInfo.getList());
            params.put("page", 1); // 固定第一页
            params.put("pageSize", pageInfo.getList().size()); // 请求全部数据

            // 7. 调用平台接口获取数据
            BaseResult<PageQueryResult<Map<String, Object>>> trainListResult = getTrainList(params);

            // 8. 处理平台接口返回结果
            if (!trainListResult.ifSuccess() || trainListResult.getData() == null) {
                log.error("获取批量发布列表失败, projectId={}, msg={}",
                        projectId, trainListResult.getMessage());
                return BaseResult.fail(BaseResultCodeEnum.SYSTEM_ERROR.getNCode(), "获取批量发布列表失败");
            }

            // 9. 重组分页结果
            PageQueryResult<Map<String, Object>> remoteResult = trainListResult.getData();
            PageQueryResult<Map<String, Object>> pageResult = new PageQueryResult<>();
            pageResult.setResultList(remoteResult.getResultList());
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

}
