package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelFileUtil;
import com.alibaba.excel.EasyExcel;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.dao.TrackMapMapper;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.facade.api.client.TrackImportService;
import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.facade.api.result.TrackImportLogVO;
import com.timevale.forward.facade.api.result.TrackImportProgressVO;
import com.timevale.forward.facade.api.result.TrackImportTemplateVO;
import com.timevale.forward.service.excel.track.TrackEvent;
import com.timevale.forward.service.excel.track.TrackListener;
import com.timevale.forward.service.excel.track.TrackRow;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.mandarin.common.service.retry.RetryCallback;
import com.timevale.mandarin.common.service.retry.RetryTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @Value("${templateFileId:b210b33a8167439e930dfe7dd3808ab8}")
    private String templateFileId;

    // 限制导入事件条数
    public static final int importEventLimit = 100;

    private static final String FILE_NAME = "E:\\test.xlsx";

    @Override
    public BaseResult<Boolean> importEvent(TrackImportReq trackImportReq) {
        List<TrackEvent> trackEventList = new ArrayList<>();

        try {
            // 获取文件流
            // InputStream in = getFileInputStream(trackImportReq.getFileId());
            InputStream in = new FileInputStream(FILE_NAME);

            // 校验文件类型
            boolean isXlsx = ExcelFileUtil.isXlsx(in);
            AssertUtil.checkState(isXlsx, "导入文件仅支持xls和xlsx格式");

            // 校验内容及格式
            EasyExcel.read(FILE_NAME, TrackRow.class, new TrackListener(trackEventList))
                    .sheet()
                    .headRowNumber(3)
                    .doRead();

            // 关闭文件流
            in.close();
        } catch (IOException e) {
            log.info("io异常");
        }

        // 事件数校验
        AssertUtil.checkState(CollectionUtil.isEmpty(trackEventList), "无有效数据，请检查后重试");
        AssertUtil.checkState(trackEventList.size() <= importEventLimit, "导入埋点事件条数不能超过" + importEventLimit);

        // 分类校验
        classifyCheck(trackEventList);
        eventNameCheck(trackEventList);

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
    public BaseResult<TrackImportTemplateVO> template() {
        TrackImportTemplateVO result = new TrackImportTemplateVO();
        result.setFileId(templateFileId);
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<PageQueryResult<TrackImportLogVO>> log() {
        return null;
    }

    /**
     * 获取文件输入流
     *
     * @param fileId 文件标识
     * @return {@link InputStream}
     */
    private InputStream getFileInputStream(String fileId) {
        // 获取下载文件流
        FileDownloadDTO info;

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
            throw new BaseBizRuntimeException("文件下载异常");
        }
        // 获取输入流
        String downloadUrl = info.getDownloadUrl();
        try {
            URL fileUrl = new URL(downloadUrl);
            URLConnection conn = fileUrl.openConnection();
            return conn.getInputStream();
        } catch (IOException e) {
            log.error("获取输出流失败，downloadUrl:{}",downloadUrl);
            throw new BaseBizRuntimeException("文件导入失败，请稍后重试");
        }
    }

    private void classifyCheck(List<TrackEvent> trackEventList) {
        // 验空
        for (TrackEvent e : trackEventList) {
            String firstClassify = e.getFirstClassify();
            if(StrUtil.isEmpty(firstClassify)) {
                e.getFailInfoList().add("【格式错误】一级分类为必填字段，请检查后修改");
            }
            String secondClassify = e.getSecondClassify();
            if(StrUtil.isEmpty(secondClassify)) {
                e.getFailInfoList().add("【格式错误】二级分类为必填字段，请检查后修改");
            }
            String thirdClassify = e.getThirdClassify();
            if(StrUtil.isEmpty(thirdClassify)) {
                e.getFailInfoList().add("【格式错误】三级分类为必填字段，请检查后修改");
            }
            String fourthClassify = e.getFourthClassify();
            if(StrUtil.isEmpty(fourthClassify)) {
                e.getFailInfoList().add("【格式错误】四级分类为必填字段，请检查后修改");
            }
        }
    }

    private void eventNameCheck(List<TrackEvent> trackEventList) {
        List<String> cnNameList = trackEventList.stream().map(TrackEvent::getEventNameCn).collect(Collectors.toList());
        List<String> egNameList = trackEventList.stream().map(TrackEvent::getEventNameEn).collect(Collectors.toList());
        List<TrackEventDO> trackEventNameDOList = trackEventMapper.selectByName(cnNameList, egNameList);

        Set<String> cnNameSet = trackEventNameDOList.stream().map(TrackEventDO::getCnName).collect(Collectors.toSet());
        Set<String> egNameSet = trackEventNameDOList.stream().map(TrackEventDO::getEgName).collect(Collectors.toSet());

        for (TrackEvent e : trackEventList) {
            String eventNameCn = e.getEventNameCn();

            // 注意， 事件中文名需要以完整分类验证
            if (StrUtil.isEmpty(eventNameCn)) {
                e.getFailInfoList().add("【格式错误】{事件中文名}为必填字段，请检查后修改");
            }
            if (eventNameCn.length() > 100) {
                e.getFailInfoList().add("【格式错误】事件中文名（含埋点分类）字数已超过100字，请检查后修改");
            }
            if (cnNameSet.contains(eventNameCn)) {
                e.getFailInfoList().add("【事件错误】事件中文名与事件“{事件中文名}”重复，请检查后修改");
            }

            String eventNameEn = e.getEventNameEn();
            if (StrUtil.isEmpty(eventNameEn)) {
                e.getFailInfoList().add("【格式错误】" + eventNameEn + "为必填字段，请检查后修改");
            }
            if (eventNameEn.length() > 100) {
                e.getFailInfoList().add("【格式错误】事件英文名字数已超过100字符，请检查后修改");
            }
            if (egNameSet.contains(eventNameEn)) {
                e.getFailInfoList().add("【事件错误】事件英文名与事件" + eventNameEn + "重复，请检查后修改");
            }
        }
    }

}
