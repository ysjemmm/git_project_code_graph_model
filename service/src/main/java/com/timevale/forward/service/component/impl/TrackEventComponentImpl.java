package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.condition.TrackEventPropCondition;
import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.dal.dao.TrackEvenPropMapper;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.dao.TrackPropMapper;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.dal.entity.TrackEventPropDO;
import com.timevale.forward.dal.entity.TrackPropDO;
import com.timevale.forward.dal.entity.TrackPropItemDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.model.enums.EnvEnum;
import com.timevale.forward.model.enums.PlatformTypeEnum;
import com.timevale.forward.model.enums.TrackPropTypeEnum;
import com.timevale.forward.service.component.TrackEventComponent;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.copy.TrackPropCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class TrackEventComponentImpl implements TrackEventComponent {

    @Resource
    private TrackEventMapper trackEventMapper;

    @Resource
    private EpeiusClient epeiusClient;

    @Resource
    private TrackEvenPropMapper trackEvenPropMapper;

    @Resource
    private TrackPropMapper trackPropMapper;

    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventListCondition condition) {
        log.info("埋点事件列表,参数:{}", condition);
        buildConditionBeforeQuery(condition);

        List<TrackEventDO> list = trackEventMapper.list(condition);
        List<TrackEventVO> trackEventVOList = TrackEventCopier.INSTANCE.convert(list);
        trackEventVOList.forEach(a -> {
            a.setStatusName(com.timevale.forward.model.enums.FlowStatusEnum.getTextByCode(a.getStatus()));
            a.setEnvNames(EnvEnum.getTextByCode(JSONObject.parseArray(a.getEnv(), Integer.class)));
            a.setPlatformNames(PlatformTypeEnum.getTextByCode(JSONObject.parseArray(a.getPlatform(), Integer.class)));
        });
        PageInfo<TrackEventDO> pageInfo = new PageInfo<>(list);
        PageQueryResult<TrackEventVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(trackEventVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public void updateTrackEventInfo(String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            log.info("流程id为空");
            return;
        }
        ProcessResponse processInfo = epeiusClient.getProcessInfo(processInstanceId);
        String processStatus = processInfo.getProcessStatus();
        log.info("返回流程信息 processInfo={}", processInfo);
        TrackEventDO trackEventDO = trackEventMapper.get(null, processInstanceId);
        if (trackEventDO == null) {
            log.info("无埋点事件流程 flowId={}", processInstanceId);
            return;
        }
        Map<String, Object> flowData = processInfo.getFlowData();
        if (FlowStatusEnum.REJECT.getValue().equals(processStatus)) {
            trackEventDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.REJECT.getCode());
            String rejectReason = flowData.get("rejectReason") == null ? StringUtils.EMPTY : String.valueOf(flowData.get("rejectReason"));
            trackEventDO.setFailReason(rejectReason);
        } else if (FlowStatusEnum.WITHDRAW.getValue().equals(processStatus)) {
            trackEventDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.WITHDRAW.getCode());

        } else if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            trackEventDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.COMPLETE.getCode());
            trackEventDO.setFailReason(StringUtils.EMPTY);
        }

        updateTrackEventProp(trackEventDO);

    }

    @Override
    public void updateTrackEventProp(TrackEventDO trackEventDO) {

        trackEventMapper.updateWithOutModifyMan(trackEventDO);

        TrackEventPropCondition c = TrackEventPropCondition.builder().trackEventId(trackEventDO.getId()).isDeleted(false).build();
        List<TrackEventPropDO> oldTrackEventPropDOList = trackEvenPropMapper.select(c);
        List<Long> oldPropIds = oldTrackEventPropDOList.stream().map(TrackEventPropDO::getTrackPropId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(oldPropIds)) {
            return;
        }
        List<TrackPropDO> trackPropDOList = trackPropMapper.selectByIds(oldPropIds);
        List<TrackPropDO> filterProps = trackPropDOList.stream().filter(a -> TrackPropTypeEnum.NEW.getCode().equals(a.getType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filterProps)) {
            return;
        }
        if (com.timevale.forward.model.enums.FlowStatusEnum.COMPLETE.getCode().equals(trackEventDO.getStatus())) {
            //更新
            List<Long> filterPropIds = filterProps.stream().map(TrackPropDO::getId).collect(Collectors.toList());
            log.info("更新事件属性 trackEventId={},filterPropIds={}", trackEventDO.getId(), filterPropIds);
            trackPropMapper.batchUpdate(filterPropIds, trackEventDO.getStatus(), TrackPropTypeEnum.EXIST.getCode());
            return;
        }

        //撤回或拒绝
        filterProps.forEach(a -> {
            TrackEventPropCondition cc = TrackEventPropCondition.builder().trackPropId(a.getId()).isDeleted(false).build();
            List<TrackEventPropDO> trackEventPropDOList = trackEvenPropMapper.select(cc);
            List<Long> otherTrackEventIds = trackEventPropDOList.stream().filter(b -> !Objects.equals(trackEventDO.getId(), b.getTrackEventId()))
                    .map(TrackEventPropDO::getTrackEventId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(otherTrackEventIds)) {
                List<Integer> status = trackEventMapper.selectByIds(otherTrackEventIds).stream().map(TrackEventDO::getStatus).collect(Collectors.toList());
                if (status.contains(com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode())
                        || status.contains(com.timevale.forward.model.enums.FlowStatusEnum.COMPLETE.getCode())) {
                    //当前属性关联其他事件
                    return;
                }
            }
            //没有关联其他事件,更新为撤回或拒绝
            if (com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode().equals(a.getStatus())) {
                log.info("更新事件属性 trackEventId={},propId={}", trackEventDO.getId(), a.getId());
                a.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.REJECT.getCode());
                trackPropMapper.updateWithOutModifyMan(a);
            }
        });
    }

    @Override
    public String startFlow(TrackEventAddReq trackEventAddReq, UserInfo userInfo) {
        StartProcessRequest start = new StartProcessRequest();
        Map<String, Object> variables = new HashMap<>();
        variables.put("files", new ArrayList<>());
        variables.put("fullCnName", trackEventAddReq.getFullCnName());
        variables.put("apiName", trackEventAddReq.getApiName());
        variables.put("egName", trackEventAddReq.getEgName());
        variables.put("platform", StringUtils.join(PlatformTypeEnum.getTextByCode(trackEventAddReq.getPlatforms()), ","));
        variables.put("touchMoment", trackEventAddReq.getTouchMoment());
        variables.put("env", StringUtils.join(EnvEnum.getTextByCode(trackEventAddReq.getEnvs()), ","));
        variables.put("trackEventName", trackEventAddReq.getFullCnName());
        variables.put("explanation", trackEventAddReq.getExplanation());

        List<FileAddReq> fileAddReqs = trackEventAddReq.getFiles();
        List<Map<String, String>> files = new ArrayList<>();
        fileAddReqs.forEach(a -> {
            Map<String, String> file = new HashMap<>();
            file.put("file_key", a.getFileKey());
            file.put("file_name", a.getFileName());
            files.add(file);
        });
        variables.put("files", files);

        List<Integer> types = Lists.newArrayList(TrackPropTypeEnum.DEFAULT.getCode());
        TrackPropListCondition c = TrackPropListCondition.builder().types(types).build();
        List<TrackPropDO> defaultProps = trackPropMapper.list(c);

        List<TrackPropDO> trackProps = TrackPropCopier.INSTANCE.change(trackEventAddReq.getTrackProps());
        trackProps.addAll(defaultProps);

        List<TrackPropItemDO> trackPropItemDOList = new ArrayList<>();
        trackProps.forEach(a -> {
            TrackPropItemDO trackPropDO = new TrackPropItemDO();
            trackPropDO.setDataType(a.getDataType());
            trackPropDO.setCnName(a.getCnName());
            trackPropDO.setEgName(a.getEgName());
            if (a.getId() != null && com.timevale.forward.model.enums.FlowStatusEnum.REJECT.getCode().equals(a.getStatus())) {
                //审核不通过的属性重新提交,变成审核中
                trackPropDO.setStatusName(com.timevale.forward.model.enums.FlowStatusEnum.getTextByCode(com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode()));
                a.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode());
                trackPropMapper.update(a);
            } else {
                trackPropDO.setStatusName(com.timevale.forward.model.enums.FlowStatusEnum.getTextByCode(a.getStatus()));
            }
            trackPropDO.setTypeName(TrackPropTypeEnum.getTextByCode(a.getType()));
            trackPropItemDOList.add(trackPropDO);
        });
        variables.put("props", trackPropItemDOList);

        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_trackEventReview");
        start.setStartAccountId(userInfo.getId());
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        String id = epeiusClient.start(start);
        return id;
    }

    @Override
    @Async("trackImportExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateFlowId(List<TrackEventDO> trackEventDOList, UserInfo userInfo) {
        log.info("[updateFlowId]:批量发送埋点审批工作流开始");
        // 开始执行工作流
        for (TrackEventDO e : trackEventDOList) {
            StartProcessRequest start = new StartProcessRequest();
            Map<String, Object> variables = new HashMap<>();
            variables.put("files", new ArrayList<>());
            variables.put("fullCnName", e.getFullCnName());
            variables.put("apiName", e.getApiName());
            variables.put("egName", e.getEgName());
            variables.put("touchMoment", e.getTouchMoment());
            variables.put("trackEventName", e.getFullCnName());
            variables.put("explanation", e.getExplanation());

            String env = e.getEnv();
            String platform = e.getPlatform();
            List<String> envSplits = StrUtil.split(env.replace("[", "".replace("]", "")), ",");
            List<String> platformSplits = StrUtil.split(platform.replace("[", "".replace("]", "")), ",");
            variables.put("env", envSplits);
            variables.put("platform", platformSplits);

            // 默认属性
            TrackPropListCondition c = TrackPropListCondition.builder()
                    .types(ListUtil.toList(TrackPropTypeEnum.DEFAULT.getCode()))
                    .build();
            List<TrackPropDO> defaultProps = trackPropMapper.list(c);

            // 关联属性
            List<TrackEventPropDO> eventPropDOList = trackEvenPropMapper.select(TrackEventPropCondition.builder().trackEventId(e.getId()).build());
            List<Long> propIdList = eventPropDOList.stream().map(TrackEventPropDO::getTrackPropId).collect(Collectors.toList());
            List<TrackPropDO> trackPropDOList = trackPropMapper.selectByIds(propIdList);
            trackPropDOList.addAll(defaultProps);

            List<TrackPropItemDO> trackPropItemDOList = new ArrayList<>();
            trackPropDOList.forEach(a -> {
                TrackPropItemDO trackPropDO = new TrackPropItemDO();
                trackPropDO.setDataType(a.getDataType());
                trackPropDO.setCnName(a.getCnName());
                trackPropDO.setEgName(a.getEgName());
                if (a.getId() != null && com.timevale.forward.model.enums.FlowStatusEnum.REJECT.getCode().equals(a.getStatus())) {
                    //审核不通过的属性重新提交,变成审核中
                    trackPropDO.setStatusName(com.timevale.forward.model.enums.FlowStatusEnum.getTextByCode(com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode()));
                    a.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode());
                    trackPropMapper.update(a);
                } else {
                    trackPropDO.setStatusName(com.timevale.forward.model.enums.FlowStatusEnum.getTextByCode(a.getStatus()));
                }
                trackPropDO.setTypeName(TrackPropTypeEnum.getTextByCode(a.getType()));
                trackPropItemDOList.add(trackPropDO);
            });
            variables.put("props", trackPropItemDOList);

            start.setApplicationName("forward");
            start.setProcessDefinitionKey("forward_trackEventReview_notNotice");
            start.setStartAccountId(userInfo.getId());
            start.setVariables(variables);
            start.setEpeVirtualProcessSwitch(false);

            try {
                String flowId = epeiusClient.start(start);

                log.error("[updateFlowId]:工作流启动成功, 事件id:{},工作流id:{}", e.getId(),flowId);
                TrackEventDO eventDO = new TrackEventDO();
                eventDO.setId(e.getId());
                eventDO.setFlowId(flowId);
                trackEventMapper.update(eventDO);
            } catch (Exception exception) {
                log.error("[updateFlowId]:工作流更新失败, 事件id:{}", e.getId());
            }
        }
        log.info("[updateFlowId]:批量发送埋点审批工作流结束");
    }

    private void buildConditionBeforeQuery(TrackEventListCondition condition) {
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        condition.setModifyDateStart(DateUtil.getStartOfDay(condition.getModifyDateStart()));
        condition.setModifyDateEnd(DateUtil.getEndOfDay(condition.getModifyDateEnd()));
    }

}
