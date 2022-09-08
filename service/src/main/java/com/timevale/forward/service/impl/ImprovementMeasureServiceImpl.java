package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ImprovementMeasureCondition;
import com.timevale.forward.dal.dao.ImprovementMeasureMapper;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.client.ImprovementMeasureService;
import com.timevale.forward.facade.api.query.ImprovementMeasureQueryList;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureCompleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureDeleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureModifyReq;
import com.timevale.forward.facade.api.result.ImprovementMeasureVO;
import com.timevale.forward.model.enums.ImprovementMeasureStatusEnum;
import com.timevale.forward.service.component.ImprovementMeasureComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ImprovementMeasureCopier;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.UpdateTodoTaskMsg;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/03/16 17:54
 */
@Slf4j
@LogPoint
@RestService
public class ImprovementMeasureServiceImpl implements ImprovementMeasureService {

    @Resource
    private ImprovementMeasureMapper improvementMeasureMapper;

    @Resource
    private DingWorkRecordClient dingWorkRecordClient;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private ImprovementMeasureComponent improvementMeasureComponent;

    public static final String TITLE = "您收到了一条任务：%s";

    @Override
    public BaseResult<Boolean> add(ImprovementMeasureAddReq improvementMeasureAddReq) {
        improvementMeasureComponent.add(improvementMeasureAddReq);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(ImprovementMeasureModifyReq improvementMeasureModifyReq) {

        // 校验是否有对应数据
        ImprovementMeasureCondition condition = ImprovementMeasureCondition.builder()
                .id(improvementMeasureModifyReq.getId())
                .isDeleted(false)
                .build();
        List<ImprovementMeasureDO> improvementMeasureDOList = improvementMeasureMapper.selectByCondition(condition);
        if(CollectionUtils.isEmpty(improvementMeasureDOList)){
            throw new BaseBizRuntimeException("该事项不存在");
        }

        // 转换后新增数据
        ImprovementMeasureDO oldImprovementMeasureDO = improvementMeasureDOList.get(0);
        ImprovementMeasureDO newImprovementMeasureDO = ImprovementMeasureCopier.INSTANCE.convert(improvementMeasureModifyReq);

        // 日期修改
        Date endOfWork = DateUtil.getEndOfWork(newImprovementMeasureDO.getImplementationTime());
        newImprovementMeasureDO.setImplementationTime(endOfWork);

        // 添加了待办事项
        Boolean oldTodo = oldImprovementMeasureDO.getTodo();
        Boolean newTodo = newImprovementMeasureDO.getTodo();

        if(oldTodo){
            // 新旧执行人是否相同
            if(Objects.equals(oldImprovementMeasureDO.getExecutorId(), newImprovementMeasureDO.getExecutorId())){
                newImprovementMeasureDO.setTodoId(oldImprovementMeasureDO.getTodoId());
                improvementMeasureComponent.updateTodoTask(newImprovementMeasureDO);
            }else{
                improvementMeasureComponent.deleteTodoTask(oldImprovementMeasureDO);

                String todoId = improvementMeasureComponent.addTodoTask(newImprovementMeasureDO);
                newImprovementMeasureDO.setTodoId(todoId);
            }
        }else if(newTodo){
            String todoId = improvementMeasureComponent.addTodoTask(newImprovementMeasureDO);
            newImprovementMeasureDO.setTodoId(todoId);
        }

        // 更新到数据库
        improvementMeasureMapper.update(newImprovementMeasureDO);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> delete(ImprovementMeasureDeleteReq improvementMeasureDeleteReq) {
        improvementMeasureComponent.delete(improvementMeasureDeleteReq.getId());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> complete(ImprovementMeasureCompleteReq improvementMeasureCompleteReq){
        // 查询是否有对应事项
        ImprovementMeasureCondition condition = ImprovementMeasureCondition.builder()
                .id(improvementMeasureCompleteReq.getId())
                .isDeleted(false)
                .build();
        List<ImprovementMeasureDO> improvementMeasureDOList = improvementMeasureMapper.selectByCondition(condition);
        if(CollectionUtils.isEmpty(improvementMeasureDOList)){
            throw new BaseBizRuntimeException("该事项不存在");
        }
        ImprovementMeasureDO improvementMeasureDO = improvementMeasureDOList.get(0);

        if(!ImprovementMeasureStatusEnum.PENDING.getCode().equals(improvementMeasureDO.getStatus())){
            throw new BaseBizRuntimeException("状态不是待处理,不能完成");
        }
        // 修改事项逻辑删除标志
        improvementMeasureDO.setStatus(ImprovementMeasureStatusEnum.COMPLETED.getCode());
        improvementMeasureDO.setContent(improvementMeasureCompleteReq.getContent());
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
    public BaseResult<PageQueryResult<ImprovementMeasureVO>> list(ImprovementMeasureQueryList improvementMeasureQueryList) {
        PageHelper.startPage(improvementMeasureQueryList.pageNum, improvementMeasureQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        // 读取数据
        ImprovementMeasureCondition condition = ImprovementMeasureCondition.builder()
                .troubleTicketId(improvementMeasureQueryList.getTroubleTicketId())
                .isDeleted(false)
                .build();
        List<ImprovementMeasureDO> improvementMeasureDOList = improvementMeasureMapper.selectByCondition(condition);

        // 更新待办状态
        improvementMeasureComponent.updateTodoStatus(improvementMeasureDOList);

        // 转换格式
        List<ImprovementMeasureVO> improvementMeasureVOList =
                improvementMeasureDOList.stream().map(ImprovementMeasureCopier.INSTANCE::convert).collect(Collectors.toList());

        // 填充枚举值描述
        improvementMeasureVOList.forEach(e -> {
            e.setStatusName(ImprovementMeasureStatusEnum.getTextByCode(e.getStatus()));
        });

        // 返回分页数据
        PageInfo<ImprovementMeasureDO> pageInfo = new PageInfo<>(improvementMeasureDOList);
        PageQueryResult<ImprovementMeasureVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(improvementMeasureVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

}
