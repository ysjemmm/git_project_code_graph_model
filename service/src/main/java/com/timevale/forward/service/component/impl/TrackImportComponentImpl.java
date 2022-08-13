package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.poi.excel.ExcelFileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.TrackImportComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.excel.track.*;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.framework.tedis.util.TedisUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.service.retry.RetryCallback;
import com.timevale.mandarin.common.service.retry.RetryTemplate;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/08/12 15:55
 */
@Slf4j
@Component
public class TrackImportComponentImpl implements TrackImportComponent {

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

    // 限制导入事件条数
    public static final int importEventLimit = 100;

    private static final String TRACK_IMPORT_CANCEL = "forward:track:import:cancel:";
    private static final String TRACK_IMPORT_PROGRESS = "forward:track:import:progress:";
    private static final String TRACK_IMPORT_RESULT = "forward:track:import:result:";

    @Override
    public Integer getProgress(String userId) {
        String progressTag = TRACK_IMPORT_PROGRESS + userId;

        Integer progress = TedisUtil.get(progressTag);
        log.info("获取用户{}导入进度：{}%", userId, progress);
        return progress;
    }

    @Override
    public void setProgress(Integer progress, String userId) {
        String progressTag = TRACK_IMPORT_PROGRESS + userId;

        log.info("更新用户{}导入进度：{}%", userId, progress);
        TedisUtil.set(progressTag, progress, 10, TimeUnit.MINUTES);
    }

    @Override
    public Boolean getCancelTag(String userId) {
        String cancelTag = TRACK_IMPORT_CANCEL + userId;

        log.info("用户{}获取取消导入操作标志", userId);
        return TedisUtil.get(cancelTag);
    }

    @Override
    public void setCancelTag(String userId) {
        String cancelTag = TRACK_IMPORT_CANCEL + userId;

        log.info("用户{}尝试取消导入操作", userId);
        TedisUtil.set(cancelTag, true, 10, TimeUnit.MINUTES);
    }

    @Override
    public void setImportResult(Integer result, String userId) {
        String resultTag = TRACK_IMPORT_RESULT + userId;

        log.info("用户{}设置导入结果：{}", userId, result);
        TedisUtil.set(resultTag, result, 10, TimeUnit.MINUTES);
    }

    @Override
    public Integer getImportResult(String userId) {
        String resultTag = TRACK_IMPORT_RESULT + userId;

        Integer result = TedisUtil.get(resultTag);
        log.info("获取用户{}导入结果：{}", userId, result);
        return result;
    }

    @Override
    public void deleteStatus(String userId) {
        String cancelTag = TRACK_IMPORT_CANCEL + userId;
        String resultTag = TRACK_IMPORT_RESULT + userId;
        String progressTag = TRACK_IMPORT_PROGRESS + userId;

        log.info("删除用户{}全部导入相关状态", userId);
        TedisUtil.delete(cancelTag, resultTag, progressTag);
    }

    @Override
    public void importEnd(String userId) {
        String cancelTag = TRACK_IMPORT_CANCEL + userId;
        String progressTag = TRACK_IMPORT_PROGRESS + userId;

        log.info("删除用户{}部分导入相关状态", userId);
        TedisUtil.delete(cancelTag, progressTag);
    }

    @SneakyThrows
    @Override
    @Async("threadPoolTaskExecutor")
    public void importEvent(TrackImportReq trackImportReq, String userId) {
        List<TrackEvent> trackEventList = new ArrayList<>();

        // 获取导入数据
        String importFileId = trackImportReq.getFileId();
        File importFile = getFile(importFileId);
        File outputFile = null;

        // 校验文件类型
        boolean isExcel = false;
        try (InputStream isXls = Files.newInputStream(importFile.toPath());
             InputStream isXlsx = Files.newInputStream(importFile.toPath())) {
            isExcel = ExcelFileUtil.isXls(isXls) || ExcelFileUtil.isXlsx(isXlsx);
        } catch (IOException e) {
            log.info("IO错误，判断是否为excel文件失败");
        }
        AssertUtil.checkState(isExcel, "导入文件仅支持xls和xlsx格式");

        try {
            // 读取合并单元格信息
            EasyExcel.read(importFile, TrackRow.class, new TrackMergeListener(trackEventList))
                    .ignoreEmptyRow(true)
                    .extraRead(CellExtraTypeEnum.MERGE)
                    .sheet()
                    .doRead();

            // 读取表头及内容
            EasyExcel.read(importFile, TrackRow.class, new TrackListener(trackEventList))
                    .ignoreEmptyRow(true)
                    .sheet()
                    .headRowNumber(3)
                    .doRead();

            // 事件数校验
            AssertUtil.checkState(CollectionUtil.isNotEmpty(trackEventList), "无有效数据，请检查后重试");
            AssertUtil.checkState(trackEventList.size() <= importEventLimit, "导入埋点事件数不能超过" + importEventLimit + "条");

            // 进度20%
            setProgress(20, userId);Thread.sleep(1000);

            // 校验
            classifyCheck(trackEventList);      setProgress(30, userId);Thread.sleep(1500);
            eventNameCheck(trackEventList);
            propCheck(trackEventList);          setProgress(50, userId);Thread.sleep(1500);
            platformCheck(trackEventList);
            touchMomentCheck(trackEventList);
            envCheck(trackEventList);           setProgress(70, userId);Thread.sleep(1500);

            // 判断是否有错误信息
            boolean failImport = trackEventList.stream().anyMatch(e -> CollectionUtil.isNotEmpty(e.getFailInfoList()));
            if (failImport) {
                // 输出错误信息
                outputFile = File.createTempFile(UUID.fastUUID().toString(), ".xlsx");
                outputFile.deleteOnExit();
                outputFailInfo(trackEventList, outputFile);
                // 导入结果
                setImportResult(TrackImportLogResultEnum.FAILURE.getCode(), userId);
            } else {
                // 通过校验，导入数据
                TrackImportLogDO trackImportLogDO = new TrackImportLogDO();
                trackImportLogDO.setStatus(TrackImportLogStatusEnum.SUCCESS.getCode());
                trackImportLogDO.setResult(TrackImportLogResultEnum.SUCCESS.getCode());
                trackImportLogDO.setImportCount(trackEventList.size());
                trackImportLogDO.setImportFailCount(0);
                trackImportLogDO.setFileId(importFileId);
                trackImportLogMapper.insert(trackImportLogDO);
                setImportResult(TrackImportLogResultEnum.SUCCESS.getCode(), userId);
            }
        } catch (IOException e) {
            log.error("埋点导入IO异常");
            throw new BaseBizRuntimeException("埋点导入处理失败");
        } finally {
            // 导入结束状态
            importEnd(userId);

            // 删除临时文件
            importFile.delete();
            if (outputFile != null) {
                outputFile.delete();
            }
        }
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
             OutputStream ous = Files.newOutputStream(importFile.toPath())){
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

    /**
     * 输出失败信息
     *
     * @param trackEventList 跟踪事件列表
     * @param outputFile     输出文件
     */
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
            EasyExcel.write(outputFile)
                    .withTemplate(ins)
                    .sheet()
                    .registerWriteHandler(new TrackMergeHandler(mergeInfo))
                    .doWrite(trackFailRowList);
        } catch (IOException e) {
            log.error("错误信息写出失败");
        }

        try (InputStream ins = Files.newInputStream(outputFile.toPath())) {
            FileDownloadDTO fileDownloadDTO = FileUtil.uploadFileToOSS(ins, "埋点事件检验错误文件.xlsx", envUtils.getEnv());
            String fileId = fileDownloadDTO.getFileId();

            TrackImportLogDO trackImportLogDO = new TrackImportLogDO();
            trackImportLogDO.setStatus(TrackImportLogStatusEnum.FAILURE.getCode());
            trackImportLogDO.setFileId(fileId);
            trackImportLogDO.setImportCount(importCount);
            trackImportLogDO.setImportFailCount(importFailCount);
            trackImportLogDO.setResult(TrackImportLogResultEnum.FAILURE.getCode());
            trackImportLogMapper.insert(trackImportLogDO);

        } catch (IOException e) {
            log.error("错误文件上传失败");
        }
    }

}
