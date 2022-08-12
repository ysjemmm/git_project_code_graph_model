package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.poi.excel.ExcelFileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.crm.sdk.common.entity.UserInfo;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.TrackImportService;
import com.timevale.forward.facade.api.query.TaskImportLogQueryList;
import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.copy.TrackImportLogCopier;
import com.timevale.forward.service.excel.track.*;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.framework.tedis.util.TedisUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.mandarin.common.service.retry.RetryCallback;
import com.timevale.mandarin.common.service.retry.RetryTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;

import javax.annotation.Resource;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/08/08 17:18
 */
@Slf4j
@LogPoint
@RestService
public class TrackImportServiceImpl implements TrackImportService {

    @Resource
    private EnvUtils envUtils;

    @Resource
    private TrackEventMapper trackEventMapper;
    @Resource
    private TrackMapMapper trackMapMapper;
    @Resource
    private BizDomainMapper bizDomainMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private ModelMapper modelMapper;
    @Resource
    private TrackImportLogMapper trackImportLogMapper;

    @Value("${templateFileId:d3a98af8ea754d11ae27d50b563d9c1f}")
    private String templateFileId;

    // 限制导入事件条数
    public static final int importEventLimit = 100;

    @Override
    public BaseResult<Boolean> importEvent(TrackImportReq trackImportReq) {
        List<TrackEvent> trackEventList = new ArrayList<>();

        File importFile = getFile(trackImportReq.getFileId());
        File outputFile = null;

        // 校验文件类型
        boolean isExcel = false;
        try (InputStream isXls = new FileInputStream(importFile);
             InputStream isXlsx = new FileInputStream(importFile)) {
            isExcel = ExcelFileUtil.isXls(isXls) || ExcelFileUtil.isXlsx(isXlsx);
        } catch (IOException e) {
            log.info("IO错误，判断是否为excel文件失败");
        }
        AssertUtil.checkState(isExcel, "导入文件仅支持xls和xlsx格式");

        try {
            // 校验合并单元格
            EasyExcel.read(importFile, TrackRow.class, new TrackMergeListener(trackEventList))
                    .ignoreEmptyRow(true)
                    .extraRead(CellExtraTypeEnum.MERGE)
                    .sheet()
                    .doRead();

            // 校验内容及格式
            EasyExcel.read(importFile, TrackRow.class, new TrackListener(trackEventList))
                    .ignoreEmptyRow(true)
                    .sheet()
                    .headRowNumber(3)
                    .doRead();

            // 事件数校验
            AssertUtil.checkState(CollectionUtil.isNotEmpty(trackEventList), "无有效数据，请检查后重试");
            AssertUtil.checkState(trackEventList.size() <= importEventLimit, "导入埋点事件数不能超过" + importEventLimit + "条");

            // 校验
            classifyCheck(trackEventList);
            eventNameCheck(trackEventList);
            propCheck(trackEventList);
            platformCheck(trackEventList);
            touchMomentCheck(trackEventList);
            envCheck(trackEventList);

            // 判断是否有错误信息
            boolean failImport = trackEventList.stream().anyMatch(e -> CollectionUtil.isNotEmpty(e.getFailInfoList()));
            if (failImport) {
                // 输出错误信息
                outputFile = File.createTempFile(UUID.fastUUID().toString(), ".xlsx");
                outputFile.deleteOnExit();
                outputFailInfo(trackEventList, outputFile);
            } else {
                System.out.println("完美通过");
            }
        } catch (IOException e) {
            log.error("埋点导入IO异常");
            throw new BaseBizRuntimeException("埋点导入处理失败");
        } finally {
            // 删除临时文件
            importFile.delete();
            if (outputFile != null) {
                outputFile.delete();
            }
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> cancel() {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TrackImportProgressVO> progress() {
        return BaseResult.success(new TrackImportProgressVO());
    }

    @Override
    public BaseResult<TrackImportLogFileVO> template() {
        FileDownloadDTO info = FileUtil.getFileDownloadInfo(templateFileId, envUtils.getEnv());
        if (info == null || StrUtil.isEmpty(info.getDownloadUrl())) {
            throw new BaseBizRuntimeException("模板文件不存在");
        }
        TrackImportLogFileVO result = TrackImportLogCopier.INSTANCE.convert(info);
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<PageQueryResult<TrackImportLogListVO>> log(TaskImportLogQueryList query) {
        PageHelper.startPage(query.pageNum, query.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        List<TrackImportLogDO> trackImportLogDOList = trackImportLogMapper.selectAll();

        List<TrackImportLogListVO> result = trackImportLogDOList.stream().map(TrackImportLogCopier.INSTANCE::convert).collect(Collectors.toList());
        result.forEach(e -> {
            e.setStatusName(TrackImportStatusEnum.getTextByCode(e.getStatus()));
            e.setResultName(TrackImportResultEnum.getTextByCode(e.getResult()));
        });

        // 文件信息
        for (int i = 0; i < result.size(); i++) {
            String fileId = trackImportLogDOList.get(i).getFileId();
            FileDownloadDTO fileDownloadInfo = FileUtil.getFileDownloadInfo(fileId, envUtils.getEnv());
            TrackImportLogFileVO fileInfo = TrackImportLogCopier.INSTANCE.convert(fileDownloadInfo);
            result.get(i).setFileInfo(fileInfo);
        }

        // 返回分页数据
        PageInfo<TrackImportLogDO> pageInfo = new PageInfo<>(trackImportLogDOList);
        PageQueryResult<TrackImportLogListVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(result);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    /**
     * 获取文件
     *
     * @param fileId 文件标识
     * @return {@link File}
     */
    private File getFile(String fileId) {
        // 获取下载文件流
        FileDownloadDTO info;
        // 重试下载
        RetryTemplate retryTemplate = new RetryTemplate();
        info = (FileDownloadDTO)retryTemplate.execute(new RetryCallback() {
            @Override
            public Object doWithRetry() {
                return FileUtil.getFileDownloadInfo(fileId, envUtils.getEnv());
            }
            @Override
            public boolean isComplete(Object result) {
                FileDownloadDTO infoResult = (FileDownloadDTO) result;
                log.info("获取文件信息请求，fileId: {}，result: {}",fileId,result);
                return StringUtils.isNotEmpty(infoResult.getDownloadUrl());
            }
        });
        if(info == null){
            log.error("文件下载异常，fileId:{}",fileId);
            throw new BaseBizRuntimeException("获取文件异常，导入失败");
        }

        // 临时文件
        File importFile;

        // 创建临时文件，输出流
        try {
            importFile = File.createTempFile(UUID.fastUUID().toString(), ".xlsx");
            importFile.deleteOnExit();
        } catch (IOException e) {
            log.error("创建临时文件失败");
            throw new BaseBizRuntimeException("获取文件异常，导入失败");
        }

        // 数据复制
        String downloadUrl = info.getDownloadUrl();
        try (InputStream ins = URLUtil.getStream(new URL(downloadUrl));
             OutputStream ous = new FileOutputStream(importFile)){
            IoUtil.copy(ins, ous);
        } catch (IOException e) {
            log.error("复制数据时失败");
            throw new BaseBizRuntimeException("获取文件异常，导入失败");
        }

        return importFile;
    }

    /**
     * 分类检查
     *
     * @param trackEventList 跟踪事件列表
     */
    private void classifyCheck(List<TrackEvent> trackEventList) {
        List<String> firstClassifyList = trackEventList.stream().map(TrackEvent::getFirstClassify).distinct().collect(Collectors.toList());

        // 业务域
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByName(firstClassifyList);
        Set<String> bizDomainNameSet = bizDomainDOList.stream().map(BizDomainDO::getName).collect(Collectors.toSet());

        // 产品线
        List<ProductLineDO> productLineDOList = productLineMapper.getByBizDomainName(new ArrayList<>(bizDomainNameSet));
        Set<String> productLineNameSet = productLineDOList.stream().map(ProductLineDO::getName).collect(Collectors.toSet());

        // 模块
        List<Long> productLineIdList = productLineDOList.stream().map(BaseDO::getId).distinct().collect(Collectors.toList());
        List<ModelDO> modelDOList = modelMapper.getByProductLineId(productLineIdList);
        Set<String> modelNameSet = modelDOList.stream().map(ModelDO::getName).collect(Collectors.toSet());

        // 页面
        List<Long> modelIdList = modelDOList.stream().map(BaseDO::getId).distinct().collect(Collectors.toList());
        List<TrackMapDO> trackPageDOList = trackMapMapper.getChildren(modelIdList, TrackMapEnum.PAGE.getCode());
        Set<String> trackPageNameSet = trackPageDOList.stream().map(TrackMapDO::getName).collect(Collectors.toSet());

        // 元素
        List<Long> trackPageIdList = trackPageDOList.stream().map(BaseDO::getId).distinct().collect(Collectors.toList());
        List<TrackMapDO> trackElementDOList = trackMapMapper.getChildren(trackPageIdList, TrackMapEnum.ELEMENT.getCode());
        Set<String> trackElementNameSet = trackElementDOList.stream().map(TrackMapDO::getName).collect(Collectors.toSet());

        // 校验
        for (TrackEvent e : trackEventList) {
            List<String> failInfoList = e.getFailInfoList();

            String eventNameCn = e.getEventNameCn();
            String firstClassify = e.getFirstClassify();
            String secondClassify = e.getSecondClassify();
            String thirdClassify = e.getThirdClassify();
            String fourthClassify = e.getFourthClassify();
            String fifthClassify = e.getFifthClassify();

            if (StrUtil.isEmpty(firstClassify)) {
                failInfoList.add("【格式错误】一级分类为必填字段，请检查后修改");
            }
            if (StrUtil.isEmpty(secondClassify)) {
                failInfoList.add("【格式错误】二级分类为必填字段，请检查后修改");
            }
            if (StrUtil.isEmpty(thirdClassify)) {
                failInfoList.add("【格式错误】三级分类为必填字段，请检查后修改");
            }
            if (StrUtil.isEmpty(fourthClassify)) {
                failInfoList.add("【格式错误】四级分类为必填字段，请检查后修改");
            }

            if (StrUtil.isNotEmpty(firstClassify) && !bizDomainNameSet.contains(firstClassify)) {
                failInfoList.add("【事件错误】一级分类不存在，请检查后修改");
            } else if (StrUtil.isNotEmpty(secondClassify) && !productLineNameSet.contains(secondClassify)) {
                failInfoList.add("【事件错误】二级分类不存在，请检查后修改");
            } else if (StrUtil.isNotEmpty(secondClassify) && !modelNameSet.contains(thirdClassify)) {
                failInfoList.add("【事件错误】三级分类不存在，请检查后修改");
            } else if (StrUtil.isNotEmpty(secondClassify) && !trackPageNameSet.contains(fourthClassify)) {
                failInfoList.add("【事件错误】四级分类不存在，请检查后修改");
            } else if (StrUtil.isNotEmpty(fifthClassify) && !trackElementNameSet.contains(fifthClassify)) {
                failInfoList.add("【事件错误】五级分类不存在，请检查后修改");
            }

            StringBuilder fullNameBuilder = new StringBuilder();
            if (StrUtil.isNotEmpty(firstClassify)) {
                fullNameBuilder.append(firstClassify).append(CommonConstant.JOIN_LINE);
            }
            if (StrUtil.isNotEmpty(secondClassify)) {
                fullNameBuilder.append(secondClassify).append(CommonConstant.JOIN_LINE);
            }
            if (StrUtil.isNotEmpty(thirdClassify)) {
                fullNameBuilder.append(thirdClassify).append(CommonConstant.JOIN_LINE);
            }
            if (StrUtil.isNotEmpty(fourthClassify)) {
                fullNameBuilder.append(fourthClassify).append(CommonConstant.JOIN_LINE);
            }
            if (StrUtil.isNotEmpty(fifthClassify)) {
                fullNameBuilder.append(fifthClassify).append(CommonConstant.JOIN_LINE);
            }
            if (StrUtil.isNotEmpty(eventNameCn)) {
                fullNameBuilder.append(eventNameCn);
            }
            e.setFullNameCn(fullNameBuilder.toString());
        }
    }

    /**
     * 事件名称检查
     *
     * @param trackEventList 跟踪事件列表
     */
    private void eventNameCheck(List<TrackEvent> trackEventList) {
        List<String> fullCnNameList = trackEventList.stream().map(TrackEvent::getFullNameCn).collect(Collectors.toList());
        List<String> egNameList = trackEventList.stream().map(TrackEvent::getEventNameEn).collect(Collectors.toList());
        List<TrackEventDO> trackEventNameDOList = trackEventMapper.selectByName(fullCnNameList, egNameList);

        Set<String> egNameSet = trackEventNameDOList.stream().map(TrackEventDO::getEgName).collect(Collectors.toSet());
        Set<String> fullCnNameSet = trackEventNameDOList.stream().map(TrackEventDO::getFullCnName).collect(Collectors.toSet());

        for (TrackEvent e : trackEventList) {
            String eventNameCn = e.getEventNameCn();
            String fullCnName = e.getFullNameCn();

            // 注意， 事件中文名需要以完整分类验证
            if (StrUtil.isEmpty(eventNameCn)) {
                e.getFailInfoList().add("【格式错误】事件中文名为必填字段，请检查后修改");
            }
            if (StrUtil.isNotEmpty(fullCnName)) {
                if (fullCnName.length() > 100) {
                    e.getFailInfoList().add("【格式错误】事件中文名（含埋点分类）字数已超过100字，请检查后修改");
                }
                if (fullCnNameSet.contains(fullCnName)) {
                    e.getFailInfoList().add("【事件错误】事件中文名与事件" + fullCnName + "重复，请检查后修改");
                }
            }

            String eventNameEn = e.getEventNameEn();
            if (StrUtil.isEmpty(eventNameEn)) {
                e.getFailInfoList().add("【格式错误】" + eventNameEn + "为必填字段，请检查后修改");
            } else {
                if (eventNameEn.length() > 100) {
                    e.getFailInfoList().add("【格式错误】事件英文名字数已超过100字符，请检查后修改");
                }
                if (egNameSet.contains(eventNameEn)) {
                    e.getFailInfoList().add("【事件错误】事件英文名与事件" + eventNameEn + "重复，请检查后修改");
                }
            }
        }
    }

    /**
     * 属性检查
     *
     * @param trackEventList 跟踪事件列表
     */
    private void propCheck(List<TrackEvent> trackEventList) {
        Set<String> dateTypeSet = Arrays.stream(TrackDataTypeEnum.values()).map(Enum::toString).collect(Collectors.toSet());
        for (TrackEvent trackEvent : trackEventList) {
            List<String> failInfoList = trackEvent.getFailInfoList();

            List<TrackProp> trackPropList = trackEvent.getTrackPropList();
            if (CollectionUtil.isEmpty(trackPropList)) {
                failInfoList.add("【格式错误】事件属性为必填字段，请检查后修改");
            }
            for (TrackProp e : trackPropList) {
                String dataType = e.getDataType();
                String propNameCn = e.getPropNameCn();
                String propNameEn = e.getPropNameEn();

                if (StrUtil.isEmpty(propNameCn)) {
                    failInfoList.add("【格式错误】属性中文名为必填字段，请检查后修改");
                } else if(propNameCn.length() > 100) {
                    failInfoList.add("【格式错误】属性中文名字数已超过100字，请检查后修改");
                }

                if (StrUtil.isEmpty(propNameEn)) {
                    failInfoList.add("【格式错误】属性英文名为必填字段，请检查后修改");
                } else if(propNameCn.length() > 100) {
                    failInfoList.add("【格式错误】属性英文名字数已超过100字，请检查后修改");
                }

                if (StrUtil.isEmpty(dataType)) {
                    failInfoList.add("【格式错误】数据类型为必填字段，请检查后修改");
                } else {
                    dataType = dataType.toUpperCase();
                    e.setDataType(dataType);
                    if(!dateTypeSet.contains(dataType)) {
                        failInfoList.add("【属性错误】属性数据类型" + dataType + "不存在，请检查后修改");
                    }
                }
            }
        }
    }

    /**
     * 平台检查
     *
     * @param trackEventList 跟踪事件列表
     */
    private void platformCheck(List<TrackEvent> trackEventList) {
        Set<String> platformSet = Arrays.stream(PlatformTypeEnum.values()).map(PlatformTypeEnum::getText).collect(Collectors.toSet());
        for (TrackEvent e : trackEventList) {
            List<String> failInfoList = e.getFailInfoList();

            String platform = e.getPlatform();
            if (StrUtil.isEmpty(platform)) {
                failInfoList.add("【格式错误】埋点平台必填字段，请检查后修改");
            } else {
                boolean failApiName = true;
                boolean failExplanation = true;

                String[] platforms = platform.split("/");
                for (String s : platforms) {
                    if(!platformSet.contains(s)) {
                        failInfoList.add("【属性错误】" + s + "埋点平台不存在，请检查后修改");
                    } else if (s.equals(PlatformTypeEnum.SERVER.getText())) {
                        String apiName = e.getApiName();
                        if (StrUtil.isEmpty(apiName) && failApiName) {
                            failApiName = false;
                            failInfoList.add("【格式错误】埋点平台含非服务端时，接口名称为必填字段，请检查后修改");
                        }
                    } else {
                        String explanation = e.getExplanation();
                        if (StrUtil.isEmpty(explanation) && failExplanation) {
                            failExplanation = false;
                            failInfoList.add("【格式错误】埋点平台含服务端时，埋点位置说明为必填字段，请检查后修改");
                        }
                    }
                }
            }
        }
    }

    /**
     * 触发时机检查
     *
     * @param trackEventList 跟踪事件列表
     */
    private void touchMomentCheck(List<TrackEvent> trackEventList) {
        for (TrackEvent e : trackEventList) {
            List<String> failInfoList = e.getFailInfoList();
            String touchMoment = e.getTouchMoment();
            if (StrUtil.isEmpty(touchMoment)) {
                failInfoList.add("【格式错误】触发时机为必填字段，请检查后修改");
            }
        }
    }

    /**
     * 所属环境检查
     *
     * @param trackEventList 跟踪事件列表
     */
    private void envCheck(List<TrackEvent> trackEventList) {
        Set<String> envSet = Arrays.stream(EnvEnum.values()).map(EnvEnum::getText).collect(Collectors.toSet());

        for (TrackEvent e : trackEventList) {
            List<String> failInfoList = e.getFailInfoList();

            String env = e.getEnv();
            if (StrUtil.isEmpty(env)) {
                failInfoList.add("【格式错误】埋点所属环境为必填字段，请检查后修改");
            } else {
                boolean failEnv = true;

                String[] envs = env.split("/");
                for (String s : envs) {
                    if (!envSet.contains(s) && failEnv) {
                        failEnv = false;
                        failInfoList.add("【属性错误】埋点所属环境为不存在，请检查后修改");
                    }
                }
            }
        }
    }

    private void outputFailInfo(List<TrackEvent> trackEventList, File outputFile) {
        List<TrackFailRow> trackFailRowList = new ArrayList<>();
        Map<Integer, Integer> mergeInfo = new HashMap<>();

        int startRow = 3;
        int importCount = trackEventList.size();
        int importFailCount = 0;

        for (TrackEvent trackEvent : trackEventList) {
            TrackFailRow trackFailRow = TrackEventCopier.INSTANCE.convert(trackEvent);

            // 新增错误信息
            List<String> failInfoList = trackEvent.getFailInfoList();
            if (CollectionUtil.isNotEmpty(failInfoList)) {
                String failInfo = String.join("\n", failInfoList);
                trackFailRow.setFailInfo(failInfo);
                importFailCount++;
            }

            List<TrackProp> trackPropList = trackEvent.getTrackPropList();
            if (CollectionUtil.isNotEmpty(trackPropList)) {

                TrackProp trackProp = trackPropList.get(0);
                trackFailRow.setPropNameCn(trackProp.getPropNameCn());
                trackFailRow.setPropNameEn(trackProp.getPropNameEn());
                trackFailRow.setDataType(trackProp.getDataType());
                trackFailRowList.add(trackFailRow);

                for (int i = 1; i < trackPropList.size(); i++) {
                    TrackFailRow row = TrackEventCopier.INSTANCE.convert(trackPropList.get(i));
                    trackFailRowList.add(row);
                }
            }

            // 保存事件合并单元格信息
            int endRow = trackPropList.size();
            mergeInfo.put(startRow, endRow);
            startRow += endRow;
        }

        ClassPathResource resource = new ClassPathResource("TRACK-FAIL-TEMPLATE.xlsx");
        try (InputStream ins = resource.getInputStream()){
            // File template = ResourceUtils.getFile("classpath:TRACK-FAIL-TEMPLATE.xlsx");

            EasyExcel.write(outputFile)
                    .withTemplate(ins)
                    .sheet()
                    .registerWriteHandler(new TrackMergeHandler(mergeInfo))
                    .doWrite(trackFailRowList);
        } catch (IOException e) {
            log.error("错误信息写出失败");
        }

        try (InputStream ins = new FileInputStream(outputFile)) {
            FileDownloadDTO fileDownloadDTO = FileUtil.uploadFileToOSS(ins, "埋点事件检验错误文件.xlsx", envUtils.getEnv());
            String fileId = fileDownloadDTO.getFileId();

            TrackImportLogDO trackImportLogDO = new TrackImportLogDO();
            trackImportLogDO.setStatus(TrackImportStatusEnum.FAILURE.getCode());
            trackImportLogDO.setFileId(fileId);
            trackImportLogDO.setImportCount(importCount);
            trackImportLogDO.setImportFailCount(importFailCount);
            trackImportLogDO.setResult(TrackImportResultEnum.FAILURE.getCode());
            trackImportLogMapper.insert(trackImportLogDO);

        } catch (IOException e) {
            log.error("错误文件上传失败");
        }
    }
}
