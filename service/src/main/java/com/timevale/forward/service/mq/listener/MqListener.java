//package com.timevale.forward.service.mq.listener;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.timevale.epeius.service.enums.FlowStatusEnum;
//import com.timevale.epeius.service.model.response.BaseFlowResponse;
//import com.timevale.epeius.service.model.response.TaskResponse;
//import com.timevale.forward.dal.dao.ProjectFlowMapper;
//import com.timevale.forward.dal.dao.ProjectMapper;
//import com.timevale.forward.dal.entity.ProjectDO;
//import com.timevale.forward.dal.entity.ProjectFlowDO;
//import com.timevale.forward.model.enums.ProjectFlowStatusEnum;
//import com.timevale.forward.service.integration.epeius.EpeiusClient;
//import com.timevale.forward.service.mq.dto.WorkflowBody;
//import com.timevale.forward.service.observer.event.FlowCompleteMsg;
//import com.timevale.forward.service.observer.event.FlowWithDrawMsg;
//import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
//import com.timevale.framework.mq.client.consumer.Listener;
//import com.timevale.framework.mq.client.consumer.ReceiveResult;
//import com.timevale.framework.mq.client.producer.Msg;
//import com.timevale.mandarin.base.util.CollectionUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.assertj.core.util.Lists;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.util.List;
//import java.util.Map;
//
///**
// * @author xingyun
// * @date 2022/05/17 19:54
// */
//@Slf4j
//@Component
//public class MqListener implements Listener {
//
//    @Resource
//    private EpeiusClient epeiusClient;
//
//    @Resource
//    MessageEventPublisher messageEventPublisher;
//
//    @Resource
//    private ProjectFlowMapper projectFlowMapper;
//
//    @Resource
//    private ProjectMapper projectMapper;
//
//
//    @Override
//    public ReceiveResult receive(List<Msg> list) {
//        for (Msg msg : list) {
//            String msgId = msg.getMsgId();
//            String message = new String(msg.getBody());
//            log.info("收到消息, msgId={}, message={}", msgId, message);
//
//            try {
//                WorkflowBody body = JSON.parseObject(message, WorkflowBody.class);
//                log.info("body: {}", JSON.toJSONString(body));
//                // 获取任务详情详情
//                TaskResponse taskResponse = epeiusClient.getTask(body.getTaskId());
//                BaseFlowResponse flowResponse = taskResponse.getFlowInfo();
//                String processInstanceId = body.getProcessInstanceId();
//                ProjectFlowDO projectFlowDO = projectFlowMapper.get(null, processInstanceId);
//                if (projectFlowDO == null) {
//                    log.info("无详设流程, flowId={}", processInstanceId);
//                    return ReceiveResult.success();
//                }
//                ProjectDO projectDO = projectMapper.get(projectFlowDO.getProjectId());
//                FlowStatusEnum flowStatus = flowResponse.getProcessStatus();
//                if (FlowStatusEnum.REJECT.equals(flowStatus)) {
//                    projectFlowDO.setStatus(ProjectFlowStatusEnum.REVIEW_FAIL.getCode());
//
//                } else if (FlowStatusEnum.WITHDRAW.equals(flowStatus)) {
//                    projectFlowDO.setStatus(ProjectFlowStatusEnum.WITHDRAW.getCode());
//                    messageEventPublisher.publish(new FlowWithDrawMsg(
//                            this,
//                            Lists.newArrayList(projectFlowDO.getProposerId()),
//                            projectDO.getName(),
//                            projectDO.getId()
//                    ));
//
//                } else if (FlowStatusEnum.FLOW_COMPLETE.equals(flowStatus)) {
//                    projectFlowDO.setStatus(ProjectFlowStatusEnum.REVIEWED.getCode());
//                    // 流程完成
//                    messageEventPublisher.publish(new FlowCompleteMsg(
//                            this,
//                            Lists.newArrayList(projectFlowDO.getProposerId()),
//                            projectDO.getName(),
//                            projectDO.getId()
//                    ));
//
//                    Map<String, Object> flowData = flowResponse.getFlowData();
//                    // 查询taskResult
//                    TaskResultCondition taskResultCondition = new TaskResultCondition();
//                    taskResultCondition.setProcessInstanceId(body.getProcessInstanceId());
//                    taskResultCondition.setTaskId(String.valueOf(flowData.get("taskId")));
//                    List<TaskResultPO> byCondition = taskResultDao.findByCondition(taskResultCondition);
//
//                    if (byCondition.isEmpty()) {
//                        log.warn("查无此任务");
//                        return ReceiveResult.success();
//                    }
//
//                    TaskResultPO taskResultPo = byCondition.get(0);
//                    // 完成任务
//                    taskResultDao.completeTask(taskResultPo.getId());
//                    Object needFollowLog = flowData.get("needFollowLog");
//                    log.info("needFollowLog :{}", needFollowLog);
//                    if (needFollowLog != null && !(boolean) needFollowLog) {
//                        return ReceiveResult.success();
//                    }
//                    // 保存跟进记录
//                    String memo = String.valueOf(flowData.get("memo"));
//                    String followStatus = flowData.get("followStatus") != null ? String.valueOf(flowData.get("followStatus")) : "SUCCESS_IN_CELL";
//                    String accessChannel = flowData.get("channel") != null ? String.valueOf(flowData.get("channel")) : "";
//                    JSONArray followFiles = flowData.get("followFiles") != null ? JSON.parseArray(JSON.toJSONString(flowData.get("followFiles"))) : null;
//                    String fileId = null, fileName = "";
//                    if (CollectionUtils.isNotEmpty(followFiles)) {
//                        JSONObject jsonObject = followFiles.getJSONObject(0);
//                        fileId = jsonObject.getString("fileId");
//                        fileName = jsonObject.getString("fileName");
//                    }
//                    String followType = String.valueOf(flowData.get("followType"));
//                    String nextFollowTime = flowData.get("nextFollowTime") != null ? String.valueOf(flowData.get("nextFollowTime")) : null;
//                    Long cid = Long.valueOf(String.valueOf(flowData.get("cid")));
//                    String supAssignee = flowData.get("supAssignee") != null ? String.valueOf(flowData.get("supAssignee")) : null;
//                    String contactUser = flowData.get("contactUser") != null ? String.valueOf(flowData.get("contactUser")) : null;
//
//                } else {
//                    log.info("[PocTestDockApprovalStrategyImpl.process()] no need to deal");
//                }
//            } catch (Exception e) {
//                log.warn("[NewWorkFlowMqConsumer.receive()] mq consume fail: ", e);
//            }
//        }
//
//        return ReceiveResult.success();
//    }
//}
