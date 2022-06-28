package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventCondition;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.TrackEventService;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.facade.api.request.TrackEventDeleteReq;
import com.timevale.forward.facade.api.request.TrackEventModifyReq;
import com.timevale.forward.facade.api.result.TrackEventDetailVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
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
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class TrackEventServiceImpl implements TrackEventService {

    @Resource
    private TrackEventMapper trackEventMapper;

    @Resource
    private TrackEvenPropMapper trackEvenPropMapper;

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
    private FileComponent fileComponent;

    @Resource
    private EpeiusClient epeiusClient;

    @Resource
    private TrackPropMapper trackPropMapper;


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

        trackEventDO.setFlowId(StringUtils.EMPTY);
        trackEventDO.setStatus(TrackStatusEnum.REVIEWING.getCode());
        trackEventMapper.insert(trackEventDO);

        List<TrackPropDO> trackProps = TrackPropCopier.INSTANCE.change(trackEventAddReq.getTrackProps());
        trackPropComponent.add(trackProps, trackEventDO.getId());

        fileComponent.add(trackEventAddReq.getFiles(), trackEventDO.getId(), FileTypeEnum.TRACK_EVENT.getCode());

        trackEventDO.setFlowId(startFlow(trackEventAddReq));
        trackEventMapper.update(trackEventDO);

        return BaseResult.success(true);
    }

    private String startFlow(TrackEventAddReq trackEventAddReq) {
        StartProcessRequest start = new StartProcessRequest();
        Map<String, Object> variables = new HashMap<>();
        variables.put("files", new ArrayList<>());
        variables.put("fullCnName", trackEventAddReq.getFullCnName());
        variables.put("egName", trackEventAddReq.getEgName());
        variables.put("platform", StringUtils.join(PlatformTypeEnum.getTextByCode(trackEventAddReq.getPlatforms()), ","));
        variables.put("touchMoment", trackEventAddReq.getTouchMoment());
        variables.put("env", StringUtils.join(EnvEnum.getTextByCode(trackEventAddReq.getEnvs()), ","));
        variables.put("trackEventName", trackEventAddReq.getFullCnName());

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
            if (a.getId() != null && TrackStatusEnum.REVIEW_FAIL.getCode().equals(a.getStatus())) {
                //审核不通过的属性重新提交,变成审核中
                trackPropDO.setStatusName(TrackStatusEnum.getTextByCode(TrackStatusEnum.REVIEWING.getCode()));
                a.setStatus(TrackStatusEnum.REVIEWING.getCode());
                trackPropMapper.update(a);
            } else {
                trackPropDO.setStatusName(TrackStatusEnum.getTextByCode(a.getStatus()));
            }
            trackPropDO.setTypeName(TrackPropTypeEnum.getTextByCode(a.getType()));
            trackPropItemDOList.add(trackPropDO);
        });
        variables.put("props", trackPropItemDOList);

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_trackEventReview");
        start.setStartAccountId(userInfo.getId());
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        String id = epeiusClient.start(start);
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(TrackEventModifyReq trackEventModifyReq) {
        log.info("埋点事件修改,参数:{}", trackEventModifyReq);
        TrackEventDO oldTrackEventDO = trackEventMapper.get(trackEventModifyReq.getId(), null);
        if (!TrackStatusEnum.WITHDRAW.getCode().equals(oldTrackEventDO.getStatus())
                && !TrackStatusEnum.REVIEW_FAIL.getCode().equals(oldTrackEventDO.getStatus())) {
            throw new BaseBizRuntimeException("状态为审核不通过,已撤回才能编辑");
        }
        checkBeforeInsert(trackEventModifyReq);

        TrackEventDO trackEventDO = TrackEventCopier.INSTANCE.convert(trackEventModifyReq);
        Long trackMapId = trackEventModifyReq.getElementId() == null ? trackEventModifyReq.getPageId() : trackEventModifyReq.getElementId();
        trackEventDO.setTrackMapId(trackMapId);

        List<TrackPropDO> trackProps = TrackPropCopier.INSTANCE.change(trackEventModifyReq.getTrackProps());
        trackPropComponent.modify(trackProps, trackEventDO.getId());
        // 附件
        fileComponent.update(trackEventModifyReq.getFiles(), trackEventModifyReq.getId(), FileTypeEnum.TRACK_EVENT.getCode());

        trackEventDO.setFlowId(startFlow(trackEventModifyReq));
        trackEventDO.setStatus(TrackStatusEnum.REVIEWING.getCode());
        trackEventMapper.update(trackEventDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TrackEventDetailVO> get(Long eventId) {
        log.info("埋点事件查看,参数:{}", eventId);
        TrackEventDO trackEventDO = trackEventMapper.get(eventId, null);
        if (trackEventDO == null) {
            throw new BaseBizRuntimeException("不存在该事件");
        }
        TrackEventDetailVO trackEventDetailVO = TrackEventCopier.INSTANCE.convert(trackEventDO);
        trackEventDetailVO.setStatusName(TrackStatusEnum.getTextByCode(trackEventDetailVO.getStatus()));
        trackEventDetailVO.setEnvNames(EnvEnum.getTextByCode(JSONObject.parseArray(trackEventDetailVO.getEnv(), Integer.class)));
        trackEventDetailVO.setPlatformNames(PlatformTypeEnum.getTextByCode(JSONObject.parseArray(trackEventDetailVO.getPlatform(), Integer.class)));
        List<TrackPropVO> trackPropVOList = trackPropComponent.get(eventId);
        trackEventDetailVO.setTrackProps(trackPropVOList);

        List<Long> elementIds = new ArrayList<>();
        List<String> elementNames = new ArrayList<>();

        buildTrackMapIds(trackEventDO.getTrackMapId(), elementIds, elementNames);

        trackEventDetailVO.setElementIds(elementIds);
        trackEventDetailVO.setElementNames(elementNames);

        return BaseResult.success(trackEventDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(TrackEventDeleteReq trackEventDeleteReq) {
        log.info("埋点事件删除,参数:{}", trackEventDeleteReq);
        TrackEventDO oldTrackEventDO = trackEventMapper.get(trackEventDeleteReq.getId(), null);

        if (TrackStatusEnum.REVIEWING.getCode().equals(oldTrackEventDO.getStatus())) {
            throw new BaseBizRuntimeException("状态为审核中不能删除");
        }
        oldTrackEventDO.setIsDeleted(true);
        trackEventMapper.update(oldTrackEventDO);
        //删除关联关系
        TrackEventPropDO trackEventPropDO = new TrackEventPropDO();
        trackEventPropDO.setTrackEventId(trackEventDeleteReq.getId());
        trackEventPropDO.setIsDeleted(true);
        trackEvenPropMapper.update(trackEventPropDO);
        return BaseResult.success(true);
    }

    private void buildTrackMapIds(Long trackMapId, List<Long> elementIds, List<String> elementNames) {
        TrackMapDO trackMapDO = trackMapMapper.get(trackMapId);
        if (trackMapDO != null) {
            Long parentId;
            TrackMapDO page = null;
            if (trackMapDO.getLevel() == 5) {
                page = trackMapMapper.get(trackMapDO.getParentId());
                parentId = page.getParentId();
            } else {
                parentId = trackMapDO.getParentId();
            }
            ModelDO modelDO = modelMapper.get(parentId);
            ProductLineDO productLineDO = productLineMapper.selectById(modelDO.getProductLineId());
            BizDomainDO bizDomainDO = bizDomainMapper.selectById(productLineDO.getBizDomainId());

            elementIds.add(bizDomainDO.getId());
            elementIds.add(productLineDO.getId());
            elementIds.add(modelDO.getId());

            elementNames.add(bizDomainDO.getName());
            elementNames.add(productLineDO.getName());
            elementNames.add(modelDO.getName());

            if (page != null) {
                elementIds.add(page.getId());
                elementNames.add(page.getName());
            }
            elementIds.add(trackMapDO.getId());
            elementNames.add(trackMapDO.getName());
        }
    }

    private void checkBeforeInsert(TrackEventAddReq trackEventAddReq) {
        List<Integer> status = Lists.newArrayList(TrackStatusEnum.REVIEWING.getCode(), TrackStatusEnum.REVIEWED.getCode());
        TrackEventCondition c = TrackEventCondition.builder().fullCnName(trackEventAddReq.getFullCnName()).status(status).build();
        List<TrackEventDO> trackEventDos = trackEventMapper.select(c);
        //sql大小写不敏感,程序判断
        boolean match = trackEventDos.stream().anyMatch(a -> Objects.equals(a.getFullCnName(), trackEventAddReq.getFullCnName()));
        if (match && !Objects.equals(trackEventAddReq.getId(), trackEventDos.get(0).getId())) {
            throw new BaseBizRuntimeException("该事件中文名重复,请修改后重试");
        }

        c = TrackEventCondition.builder().egName(trackEventAddReq.getEgName()).status(status).build();
        trackEventDos = trackEventMapper.select(c);
        match = trackEventDos.stream().anyMatch(a -> Objects.equals(a.getEgName(), trackEventAddReq.getEgName()));
        if (match && !Objects.equals(trackEventAddReq.getId(), trackEventDos.get(0).getId())) {
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
                return Lists.newArrayList(trackMapId);
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
