package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.condition.TrackEventPropCondition;
import com.timevale.forward.dal.dao.TrackEvenPropMapper;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.dao.TrackPropMapper;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.dal.entity.TrackEventPropDO;
import com.timevale.forward.dal.entity.TrackPropDO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.model.enums.EnvEnum;
import com.timevale.forward.model.enums.PlatformTypeEnum;
import com.timevale.forward.model.enums.TrackPropTypeEnum;
import com.timevale.forward.model.enums.TrackStatusEnum;
import com.timevale.forward.service.component.TrackEventComponent;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        List<TrackEventDO> list = trackEventMapper.list(condition);
        List<TrackEventVO> trackEventVOList = TrackEventCopier.INSTANCE.convert(list);
        trackEventVOList.forEach(a -> {
            a.setStatusName(TrackStatusEnum.getTextByCode(a.getStatus()));
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
            trackEventDO.setStatus(TrackStatusEnum.REVIEW_FAIL.getCode());
            String rejectReason = flowData.get("rejectReason") == null ? "" : String.valueOf(flowData.get("rejectReason"));
            trackEventDO.setFailReason(rejectReason);
        } else if (FlowStatusEnum.WITHDRAW.getValue().equals(processStatus)) {
            trackEventDO.setStatus(TrackStatusEnum.WITHDRAW.getCode());

        } else if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            trackEventDO.setStatus(TrackStatusEnum.REVIEWED.getCode());
        }

        updateTrackEventProp(trackEventDO);

    }

    @Override
    public void updateTrackEventProp(TrackEventDO trackEventDO) {

        trackEventMapper.update(trackEventDO);

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
        if (TrackStatusEnum.REVIEWED.getCode().equals(trackEventDO.getStatus())) {
            //更新
            List<Long> filterPropIds = filterProps.stream().map(TrackPropDO::getId).collect(Collectors.toList());
            log.info("更新事件属性 trackEventId={},filterPropIds={}", trackEventDO.getId(),filterPropIds);
            trackPropMapper.batchUpdate(filterPropIds,trackEventDO.getStatus(),TrackPropTypeEnum.EXIST.getCode());
            return;
        }
        //撤回或拒绝 找出没有关联其他事件的属性,打上相应状态
        filterProps.forEach(a -> {
            TrackEventPropCondition cc = TrackEventPropCondition.builder().trackPropId(a.getId()).isDeleted(false).build();
            List<TrackEventPropDO> trackEventPropDOList = trackEvenPropMapper.select(cc);
            List<Long> trackEventIds = trackEventPropDOList.stream().filter(b -> !Objects.equals(trackEventDO.getId(), b.getTrackEventId()))
                    .map(TrackEventPropDO::getTrackEventId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(trackEventIds)) {
                List<TrackEventDO> trackEventDos = trackEventMapper.selectByIds(trackEventIds);
                if(CollectionUtils.isNotEmpty(trackEventDos)){
                    //被其他事件关联,不用更新状态
                    return;
                }
            }
            //更新
            if(TrackStatusEnum.REVIEWING.getCode().equals(a.getStatus())){
                log.info("更新事件属性 trackEventId={},propIds={}", trackEventDO.getId(),a.getId());
                a.setStatus(trackEventDO.getStatus());
                trackPropMapper.update(a);
            }
        });

    }


}
