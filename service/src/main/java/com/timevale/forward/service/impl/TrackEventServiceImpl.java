package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductDemandTrackEventCondition;
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
import com.timevale.forward.facade.api.result.TrackExportLogFileVO;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.copy.TrackPropCopier;
import com.timevale.forward.service.excel.track.sensor.SensorTrackOutputStrategy;
import com.timevale.forward.service.excel.track.sensor.SensorTrackRow;
import com.timevale.forward.service.excel.track.sensor.SensorTrackStyleStrategy;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
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

    @Resource
    private ProductDemandLogComponent productDemandLogComponent;

    @Resource
    private ProductDemandTrackEventMapper productDemandTrackEventMapper;

    @Resource
    private ProductDemandTrackEventComponent productDemandTrackEventComponent;

    @Resource
    private EnvUtils envUtils;

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
        trackEventDO.setStatus(FlowStatusEnum.AUDITING.getCode());
        trackEventMapper.insert(trackEventDO);

        List<TrackPropDO> trackProps = TrackPropCopier.INSTANCE.change(trackEventAddReq.getTrackProps());
        trackPropComponent.add(trackProps, trackEventDO.getId());

        fileComponent.add(trackEventAddReq.getFiles(), trackEventDO.getId(), FileTypeEnum.TRACK_EVENT.getCode());

        trackEventDO.setFlowId(trackEventComponent.startFlow(trackEventAddReq, LocalSessionUtils.getUserInfo()));
        trackEventMapper.update(trackEventDO);

        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(TrackEventModifyReq trackEventModifyReq) {
        log.info("埋点事件修改,参数:{}", trackEventModifyReq);
        TrackEventDO oldTrackEventDO = trackEventMapper.get(trackEventModifyReq.getId(), null);
        if (!FlowStatusEnum.WITHDRAW.getCode().equals(oldTrackEventDO.getStatus())
                && !FlowStatusEnum.REJECT.getCode().equals(oldTrackEventDO.getStatus())) {
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

        trackEventDO.setFlowId(trackEventComponent.startFlow(trackEventModifyReq, LocalSessionUtils.getUserInfo()));
        trackEventDO.setStatus(FlowStatusEnum.AUDITING.getCode());
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
        trackEventDetailVO.setStatusName(FlowStatusEnum.getTextByCode(trackEventDetailVO.getStatus()));
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

        if (FlowStatusEnum.AUDITING.getCode().equals(oldTrackEventDO.getStatus())) {
            throw new BaseBizRuntimeException("状态为审核中不能删除");
        }
        oldTrackEventDO.setIsDeleted(true);
        trackEventMapper.update(oldTrackEventDO);
        //删除关联关系
        TrackEventPropDO trackEventPropDO = new TrackEventPropDO();
        trackEventPropDO.setTrackEventId(trackEventDeleteReq.getId());
        trackEventPropDO.setIsDeleted(true);
        trackEvenPropMapper.update(trackEventPropDO);

        ProductDemandTrackEventCondition c = ProductDemandTrackEventCondition.builder().trackEventId(trackEventDeleteReq.getId()).isDeleted(false).build();
        List<Long> productDemandIds = productDemandTrackEventMapper.select(c).stream().map(ProductDemandTrackEventDO::getProductDemandId).collect(Collectors.toList());
        productDemandIds.forEach(a->{
            productDemandTrackEventComponent.update(a,oldTrackEventDO.getId());
            productDemandLogComponent.addLogWhenLinkOrUnlinkTrackEvent(a, Lists.newArrayList(oldTrackEventDO.getFullCnName()), ButtonActionEnum.UN_LINK.getText());
        });
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TrackExportLogFileVO> exportList(TrackEventQueryList trackEventQueryList) {
        // 完整查询
        log.info("埋点事件列表,参数:{}", trackEventQueryList);

        // 埋点地图参数
        TrackEventListCondition condition = TrackEventCopier.INSTANCE.convert(trackEventQueryList);
        List<Long> trackMapChildrenWithSelf = getTrackMapChildrenWithSelf(trackEventQueryList);
        if (trackEventQueryList.getTrackMapId() != null && CollectionUtils.isEmpty(trackMapChildrenWithSelf)) {
            throw new BaseBizRuntimeException("需要导出数据为空，请检查后重试");
        }
        condition.setTrackMapIds(trackMapChildrenWithSelf);

        // 查询埋点事件，判空
        List<TrackEventVO> trackEventVOList = trackEventComponent.listAll(condition);
        if (CollUtil.isEmpty(trackEventVOList)) {
            throw new BaseBizRuntimeException("需要导出数据为空，请检查后重试");
        }
        log.info("[TrackEventServiceImpl.exportList]需要导出的事件数:{}",trackEventVOList.size());

        // 查询埋点关联属性
        List<Long> trackEventIdList = trackEventVOList.stream().map(TrackEventVO::getId).collect(Collectors.toList());
        List<TrackEventPropDO> trackEventPropDOList = trackEvenPropMapper.selectByEventIdList(trackEventIdList);

        // 查询对应属性
        List<Long> trackPropIdList = trackEventPropDOList.stream().map(TrackEventPropDO::getTrackPropId).distinct().collect(Collectors.toList());
        List<TrackPropDO> trackPropDOList = new ArrayList<>();
        if (CollUtil.isNotEmpty(trackPropIdList)) {
            trackPropDOList = trackPropMapper.selectByIds(trackPropIdList);
        }
        log.info("[TrackEventServiceImpl.exportList]事件相关的属性数量:{}",trackPropDOList.size());

        // 属性id-属性 Map
        Map<Long, TrackPropDO> trackPropMap = trackPropDOList.stream().collect(Collectors.toMap(BaseDO::getId, Function.identity(), (a, b) -> a));
        // 事件id-属性idList Map
        Map<Long, List<TrackEventPropDO>> trackEventPropMap = trackEventPropDOList.stream().collect(Collectors.groupingBy(TrackEventPropDO::getTrackEventId));

        // 数据处理，firstRow指模板中写入的起始行，serialNumber指事件编号
        int firstRow = 3;
        long serialNumber = 1L;
        Map<Integer, Integer> mergeInfo = new HashMap<>();

        List<SensorTrackRow> sensorTrackRowList = new ArrayList<>();
        for (TrackEventVO event : trackEventVOList) {
            // 基础属性
            SensorTrackRow eventRow = new SensorTrackRow();
            eventRow.setSerialNumber(serialNumber);
            eventRow.setEventNameEn(event.getEgName());
            eventRow.setEventNameCn(event.getFullCnName());
            eventRow.setPropNameCn("$预置属性");
            eventRow.setTouchMoment(event.getTouchMoment());

            // 埋点平台
            List<String> platformNames = event.getPlatformNames();
            String platformNamesStr = String.join("/", platformNames);
            eventRow.setPlatform(platformNamesStr);

            // 添加到导入列表中
            sensorTrackRowList.add(eventRow);

            // 获取与属性的关联关系
            Long eventId = event.getId();
            List<TrackEventPropDO> linkList = trackEventPropMap.get(eventId);
            if (CollUtil.isNotEmpty(linkList)) {
                // 遍历填充数据
                List<Long> propIdList = linkList.stream().map(TrackEventPropDO::getTrackPropId).collect(Collectors.toList());
                for (Long propId : propIdList) {
                    TrackPropDO trackPropDO = trackPropMap.get(propId);
                    // 验空
                    if (trackPropDO == null) {
                        continue;
                    }
                    // 填充数据
                    SensorTrackRow propRow = new SensorTrackRow();
                    propRow.setSerialNumber(serialNumber);
                    propRow.setEventNameEn(event.getEgName());
                    propRow.setEventNameCn(event.getFullCnName());
                    propRow.setTouchMoment(event.getTouchMoment());
                    propRow.setPropNameEn(trackPropDO.getEgName());
                    propRow.setPropNameCn(trackPropDO.getCnName());
                    propRow.setDataType(trackPropDO.getDataType());
                    propRow.setPlatform(platformNamesStr);
                    // 添加到导入列表中
                    sensorTrackRowList.add(propRow);
                }
            }

            // 合并行信息，事件编号
            mergeInfo.put(firstRow, firstRow + CollUtil.size(linkList));
            firstRow += CollUtil.size(linkList) + 1;
            serialNumber ++;
        }

        log.info("[TrackEventServiceImpl.uploadFile]合并单元格信息mergeInfo:{}",mergeInfo);
        TrackExportLogFileVO trackExportLogFileVO = uploadFile(sensorTrackRowList, mergeInfo);

        return BaseResult.success(trackExportLogFileVO);
    }

    private TrackExportLogFileVO uploadFile(List<SensorTrackRow> sensorTrackRowList, Map<Integer, Integer> mergeInfo) {
        File tempFile = null;
        InputStream tempFileIns = null;
        InputStream templateIns = null;
        TrackExportLogFileVO result = new TrackExportLogFileVO();
        try {
            tempFile = File.createTempFile("埋点事件导出", ".xlsx");
            tempFile.deleteOnExit();
            log.info("[TrackEventServiceImpl.uploadFile]埋点事件导出临时文件创建成功");

            // 读取模板文件
            ClassPathResource resource = new ClassPathResource("TRACK-EXPORT-TEMPLATE.xlsx");
            templateIns = resource.getInputStream();

            // 数据写入到临时文件
            EasyExcel.write(tempFile)
                    .withTemplate(templateIns)
                    .relativeHeadRowIndex(0)
                    .registerWriteHandler(new SensorTrackOutputStrategy(mergeInfo))
                    .registerWriteHandler(new SensorTrackStyleStrategy())
                    .sheet("自定义事件表")
                    .doWrite(sensorTrackRowList);
            log.info("[TrackEventServiceImpl.uploadFile]数据写入临时文件成功");

            // 上传文件
            tempFileIns = Files.newInputStream(tempFile.toPath());
            FileDownloadDTO info = FileUtil.uploadFileToOSS(tempFileIns, "埋点事件表.xlsx", envUtils.getEnv());
            if (info == null || StrUtil.isEmpty(info.getDownloadUrl())) {
                throw new BaseBizRuntimeException("埋点事件导出失败");
            }
            log.info("[TrackEventServiceImpl.uploadFile]文件上传成功");

            // 结果转化
            result = TrackEventCopier.INSTANCE.convert(info);
        } catch (IOException e) {
            throw new BaseBizRuntimeException("埋点事件导出失败");
        } finally {
            // 关闭IO且删除临时文件
            IoUtil.closeIfPosible(tempFileIns);
            IoUtil.closeIfPosible(templateIns);
            if (tempFile != null && !tempFile.delete()) {
                log.error("[TrackImportServiceImpl][template]:模板临时文件删除失败");
            }
        }

        // 返回文件
        return result;
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
        List<Integer> status = Lists.newArrayList(FlowStatusEnum.AUDITING.getCode(), FlowStatusEnum.COMPLETE.getCode());
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
