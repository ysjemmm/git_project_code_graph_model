package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ImprovementMeasureMapper;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.client.ImprovementMeasureService;
import com.timevale.forward.facade.api.query.ImprovementMeasureQueryList;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureCompleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureDeleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureModifyReq;
import com.timevale.forward.facade.api.result.ImprovementMeasureVO;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import com.timevale.forward.model.enums.ImprovementMeasureStatusEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ImprovementMeasureCopier;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.CreateTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.DeleteTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.UpdateTodoTaskMsg;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/03/16 17:54
 */
@Slf4j
@RestService
public class ImprovementMeasureServiceImpl implements ImprovementMeasureService {

    @Resource
    ImprovementMeasureMapper improvementMeasureMapper;

    @Resource
    DingWorkRecordClient dingWorkRecordClient;

    @Resource
    InnerUserPersonClient innerUserPersonClient;

    public static final String TITLE = "您收到了一条任务：%s";

    @Override
    public BaseResult<Boolean> add(ImprovementMeasureAddReq improvementMeasureAddReq) {
        log.info("改进措施-新增 add 参数:{}", improvementMeasureAddReq);

        // 转换后新增数据
        ImprovementMeasureDO improvementMeasureDO = ImprovementMeasureCopier.INSTANCE.convert(improvementMeasureAddReq);

        // 查看是否创建待办
        if(improvementMeasureDO.getTodo()){
            // 获取 unionId
            String userId = LocalSessionUtils.getUserInfo().getId();
            String executorId = improvementMeasureDO.getExecutorId();
            Map<String, String> unionIdMap = innerUserPersonClient.getUnionIds(Lists.newArrayList(userId,executorId));
            if (CollectionUtils.isEmpty(unionIdMap)) {
                log.info("新增待办时,查询用户中心所属用户无unionId");
            }
            String userUnionId = unionIdMap.get(userId);
            String executorUnionId = unionIdMap.get(executorId);

            // 发送待办
            CreateTodoTaskMsg todoTaskMsg = CreateTodoTaskMsg.builder()
                    .title(String.format(TITLE, improvementMeasureDO.getName()))
                    .unionId(userUnionId)
                    .executorIds(Lists.newArrayList(executorUnionId))
                    .dueTime(improvementMeasureDO.getImplementationTime().getTime())
                    .build();
            String todoId = dingWorkRecordClient.addTask(todoTaskMsg);

            // 保存待办id
            if (StringUtils.isEmpty(todoId)) {
                log.info("新增待办异常,createTodoTaskMsg :{}", todoTaskMsg);
                improvementMeasureDO.setTodo(false);
            }
            improvementMeasureDO.setTodoId(todoId);
        }

        // 保存到数据库
        improvementMeasureDO.setStatus(ImprovementMeasureStatusEnum.PENDING.getCode());
        improvementMeasureMapper.insert(improvementMeasureDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(ImprovementMeasureModifyReq improvementMeasureModifyReq) {
        log.info("改进措施-修改 modify 参数:{}", improvementMeasureModifyReq);

        // 校验是否有对应数据
        Long id = improvementMeasureModifyReq.getId();
        ImprovementMeasureDO oldImprovementMeasureDO = improvementMeasureMapper.selectById(id);
        if(oldImprovementMeasureDO == null){
            throw new BaseBizRuntimeException("该事项不存在");
        }

        // 转换后新增数据
        ImprovementMeasureDO newImprovementMeasureDO = ImprovementMeasureCopier.INSTANCE.convert(improvementMeasureModifyReq);

        // 更新到数据库
        improvementMeasureMapper.update(newImprovementMeasureDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> delete(ImprovementMeasureDeleteReq improvementMeasureDeleteReq) {
        // 查询是否有对应事项
        Long id = improvementMeasureDeleteReq.getId();
        ImprovementMeasureDO improvementMeasureDO = improvementMeasureMapper.selectById(id);
        if(improvementMeasureDO == null){
            throw new BaseBizRuntimeException("该事项不存在");
        }

        // 待办处理
        if(improvementMeasureDO.getTodo()){
            // 获取 用户 unionId
            String executorId = improvementMeasureDO.getExecutorId();
            Map<String, String> unionIdMap = innerUserPersonClient.getUnionIds(Lists.newArrayList(executorId));
            if (CollectionUtils.isEmpty(unionIdMap)) {
                log.info("删除待办时,查询用户中心所属用户无unionId");
            }
            String unionId = unionIdMap.get(executorId);

            DeleteTodoTaskMsg deleteTodoTaskMsg = DeleteTodoTaskMsg.builder()
                    .recordId(improvementMeasureDO.getTodoId())
                    .unionId(unionId)
                    .build();
            dingWorkRecordClient.deleteTask(deleteTodoTaskMsg);

            log.info("删除待办,deleteTodoTaskMsg:{}", deleteTodoTaskMsg);
        }

        // 修改事项逻辑删除标志
        improvementMeasureDO.setIsDeleted(true);
        improvementMeasureMapper.update(improvementMeasureDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> complete(ImprovementMeasureCompleteReq improvementMeasureCompleteReq) {
        // 查询是否有对应事项
        Long id = improvementMeasureCompleteReq.getId();
        ImprovementMeasureDO improvementMeasureDO = improvementMeasureMapper.selectById(id);
        if(improvementMeasureDO == null){
            throw new BaseBizRuntimeException("该事项不存在");
        }

        // 修改事项逻辑删除标志
        improvementMeasureDO.setStatus(ImprovementMeasureStatusEnum.COMPLETED.getCode());
        improvementMeasureMapper.update(improvementMeasureDO);

        // 待办处理
        if(improvementMeasureDO.getTodo()){
            // 获取 unionId
            String executorId = improvementMeasureDO.getExecutorId();
            Map<String, String> unionIdMap = innerUserPersonClient.getUnionIds(Lists.newArrayList(executorId));
            if (CollectionUtils.isEmpty(unionIdMap)) {
                log.info("删除待办时,查询用户中心所属用户无unionId");
            }
            String unionId = unionIdMap.get(executorId);

            // 更新状态
            UpdateTodoTaskMsg updateTodoTaskMsg = UpdateTodoTaskMsg.builder()
                    .recordId(improvementMeasureDO.getTodoId())
                    .title(String.format(TITLE, improvementMeasureDO.getName()))
                    .unionId(unionId)
                    .executorIds(Lists.newArrayList(unionId))
                    .participantIds(Lists.newArrayList(unionId))
                    .done(true)
                    .dueTime(improvementMeasureDO.getImplementationTime().getTime())
                    .build();
            dingWorkRecordClient.updateTask(updateTodoTaskMsg);

            log.info("更新待办为已完成,improvementMeasureDO:{}", improvementMeasureDO);
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<ImprovementMeasureVO>> list(ImprovementMeasureQueryList improvementMeasureQueryList) {
        PageHelper.startPage(improvementMeasureQueryList.pageNum, improvementMeasureQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        // 读取数据，转换格式
        List<ImprovementMeasureDO> improvementMeasureDOList =
                improvementMeasureMapper.selectByTroubleTicketId(improvementMeasureQueryList.getTroubleTicketId());
        List<ImprovementMeasureVO> improvementMeasureVOList =
                improvementMeasureDOList.stream().map(ImprovementMeasureCopier.INSTANCE::convert).collect(Collectors.toList());

        // 填充枚举值描述
        improvementMeasureVOList.forEach(e -> {
            e.setStatusName(ImprovementMeasureStatusEnum.getTextByCode(e.getStatus()));
        });

        // 更新待办状态

        return BaseResult.success(improvementMeasureVOList);
    }
}
