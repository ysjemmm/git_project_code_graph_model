package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.timevale.epeius.service.model.request.TerminateRequest;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventCondition;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.TrackEventService;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.facade.api.request.TrackEventDeleteReq;
import com.timevale.forward.facade.api.request.TrackEventModifyReq;
import com.timevale.forward.facade.api.result.TrackEventDetailVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.forward.model.enums.EnvEnum;
import com.timevale.forward.model.enums.PlatformTypeEnum;
import com.timevale.forward.model.enums.TrackMapEnum;
import com.timevale.forward.model.enums.TrackStatusEnum;
import com.timevale.forward.service.component.TrackEventComponent;
import com.timevale.forward.service.component.TrackPropComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.copy.TrackPropCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class TrackEventServiceImpl implements TrackEventService {

    /**
     * 流程审批人
     */
    @Value("${default.trackReviewer:chenran}")
    private String trackReviewer;

    @Resource
    private TrackEventMapper trackEventMapper;

    @Resource
    private TrackMapMapper trackMapMapper;

    @Resource
    private TrackEventComponent trackEventComponent;

    @Resource
    private TrackPropComponent trackPropComponent;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private ModelMapper modelMapper;

    @Resource
    private EpeiusClient epeiusClient;


    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventQueryList trackEventQueryList) {
        log.info("埋点事件列表,参数:{}", trackEventQueryList);
        TrackEventListCondition condition = TrackEventCopier.INSTANCE.convert(trackEventQueryList);
        List<Long> trackMapChildrenWithSelf = getTrackMapChildrenWithSelf(trackEventQueryList);
        if (trackEventQueryList.getTrackMapId() != null && CollectionUtils.isEmpty(trackMapChildrenWithSelf)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        condition.setTrackMapIds(trackMapChildrenWithSelf);
        PageHelper.startPage(trackEventQueryList.getPageNum(), trackEventQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return trackEventComponent.list(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(TrackEventAddReq trackEventAddReq) {
        log.info("埋点事件新增,参数:{}", trackEventAddReq);
        checkBeforeInsert(trackEventAddReq);
        TrackEventDO trackEventDO = TrackEventCopier.INSTANCE.convert(trackEventAddReq);
        Long trackMapId = trackEventAddReq.getElementId() == null ? trackEventAddReq.getPageId() : trackEventAddReq.getElementId();
        trackEventDO.setTrackMapId(trackMapId);
        trackEventDO.setFlowId("");
        trackEventDO.setStatus(TrackStatusEnum.REVIEWING.getCode());
        trackEventMapper.insert(trackEventDO);

        List<TrackPropDO> trackProps = TrackPropCopier.INSTANCE.change(trackEventAddReq.getTrackProps());
        trackPropComponent.add(trackProps, trackEventDO.getId());
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(TrackEventModifyReq trackEventModifyReq) {
        log.info("埋点事件修改,参数:{}", trackEventModifyReq);
        checkBeforeInsert(trackEventModifyReq);
        TrackEventDO trackEventDO = TrackEventCopier.INSTANCE.convert(trackEventModifyReq);
        Long trackMapId = trackEventModifyReq.getElementId() == null ? trackEventModifyReq.getPageId() : trackEventModifyReq.getElementId();
        trackEventDO.setTrackMapId(trackMapId);
        trackEventMapper.update(trackEventDO);
        List<TrackPropDO> trackProps = TrackPropCopier.INSTANCE.change(trackEventModifyReq.getTrackProps());
        trackPropComponent.modify(trackProps, trackEventDO.getId());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TrackEventDetailVO> get(Long eventId) {
        TrackEventDO trackEventDO = trackEventMapper.get(eventId);
        if (trackEventDO == null) {
            throw new BaseBizRuntimeException("不存在该事件");
        }
        TrackEventDetailVO trackEventDetailVO = TrackEventCopier.INSTANCE.convert(trackEventDO);
        trackEventDetailVO.setStatusName(TrackStatusEnum.getTextByCode(trackEventDetailVO.getStatus()));
        trackEventDetailVO.setEnvNames(EnvEnum.getTextByCode(JSONObject.parseArray(trackEventDetailVO.getEnv(), Integer.class)));
        trackEventDetailVO.setPlatformNames(PlatformTypeEnum.getTextByCode(JSONObject.parseArray(trackEventDetailVO.getPlatform(), Integer.class)));
        List<TrackPropVO> trackPropVOList = trackPropComponent.get(eventId);

        trackEventDetailVO.setTrackProps(trackPropVOList);
        return BaseResult.success(trackEventDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(TrackEventDeleteReq trackEventDeleteReq) {
        TrackEventDO oldTrackEventDO = trackEventMapper.get(trackEventDeleteReq.getId());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (trackEventDeleteReq.getType() == 0) {
            if (!Objects.equals(userInfo.getId(), trackReviewer) && !TrackStatusEnum.canDelete(oldTrackEventDO.getStatus())) {
                //审核人任意状态可删除,其他人审核不通过or已撤回可删除
                throw new BaseBizRuntimeException("状态为已撤回才能删除");
            }
            oldTrackEventDO.setIsDeleted(true);
            trackEventMapper.update(oldTrackEventDO);

        } else {
            if (!TrackStatusEnum.REVIEWING.getCode().equals(oldTrackEventDO.getStatus())) {
                throw new BaseBizRuntimeException("状态为审核中才能撤回");
            }
        }
        TerminateRequest request = new TerminateRequest();
        request.setProcessInstanceId(oldTrackEventDO.getFlowId());
        request.setAssignee(userInfo.getId());
//        epeiusClient.withdrawInstance(request);
        return BaseResult.success(true);
    }

    private void checkBeforeInsert(TrackEventAddReq trackEventAddReq) {
        TrackEventCondition c = TrackEventCondition.builder().cnName(trackEventAddReq.getCnName()).build();
        List<TrackEventDO> trackEventDos = trackEventMapper.select(c);
        if (!CollectionUtils.isEmpty(trackEventDos) && !Objects.equals(trackEventAddReq.getId(), trackEventDos.get(0).getId())) {
            throw new BaseBizRuntimeException("该事件中文名重复,请修改后重试");
        }

        c = TrackEventCondition.builder().egName(trackEventAddReq.getEgName()).build();
        trackEventDos = trackEventMapper.select(c);
        if (!CollectionUtils.isEmpty(trackEventDos) && !Objects.equals(trackEventAddReq.getId(), trackEventDos.get(0).getId())) {
            throw new BaseBizRuntimeException("该事件英文名重复,请修改后重试");
        }
    }

    private List<Long> getTrackMapChildrenWithSelf(TrackEventQueryList trackEventQueryList) {
        Long trackMapId = trackEventQueryList.getTrackMapId();
        Integer level = trackEventQueryList.getLevel();
        if (level != null && trackMapId != null) {
            if (TrackMapEnum.BIZDOMAIN.getCode().equals(level)) {
                BizDomainDO bizDomainDO = bizDomainMapper.selectById(trackMapId);
                List<Long> productLineIds = productLineMapper.getBizDomainId(bizDomainDO.getId()).stream().map(ProductLineDO::getId).collect(Collectors.toList());
                return getByProductLineIds(productLineIds);
            }
            if (TrackMapEnum.PRODUCTLINE.getCode().equals(level)) {
                return getByProductLineIds(Lists.newArrayList(trackMapId));
            }
            if (TrackMapEnum.MODE.getCode().equals(level)) {
                return getByModelIds(Lists.newArrayList(trackMapId));
            }
            if (TrackMapEnum.PAGE.getCode().equals(level)) {
                return getByPageIds(Lists.newArrayList(trackMapId));
            }
            if (TrackMapEnum.ELEMENT.getCode().equals(level)) {
                List<TrackMapDO> element = trackMapMapper.getChildren(Lists.newArrayList(trackMapId), TrackMapEnum.ELEMENT.getCode());
                return element.stream().map(TrackMapDO::getId).collect(Collectors.toList());
            }
        }
        return Lists.emptyList();
    }

    private List<Long> getByProductLineIds(List<Long> productLineIds) {
        if (CollectionUtils.isEmpty(productLineIds)) {
            return productLineIds;
        }
        List<Long> modelIds = modelMapper.getByProductLineId(productLineIds).stream().map(ModelDO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(modelIds)) {
            return modelIds;
        }
        return getByModelIds(modelIds);

    }

    private List<Long> getByModelIds(List<Long> modelIds) {
        if (CollectionUtils.isEmpty(modelIds)) {
            return modelIds;
        }
        List<Long> pageIds = trackMapMapper.getChildren(modelIds, TrackMapEnum.PAGE.getCode()).stream().map(TrackMapDO::getId).collect(Collectors.toList());
        return getByPageIds(pageIds);
    }

    private List<Long> getByPageIds(List<Long> pageIds) {
        if (CollectionUtils.isEmpty(pageIds)) {
            return pageIds;
        }
        List<Long> result = new ArrayList<>(pageIds);
        List<Long> elementIds = trackMapMapper.getChildren(pageIds, TrackMapEnum.ELEMENT.getCode()).stream().map(TrackMapDO::getId).collect(Collectors.toList());
        result.addAll(elementIds);
        return result;
    }
}
