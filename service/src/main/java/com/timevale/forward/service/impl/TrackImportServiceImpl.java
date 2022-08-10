package com.timevale.forward.service.impl;

import cn.hutool.poi.excel.ExcelFileUtil;
import com.alibaba.excel.EasyExcel;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.TrackImportService;
import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.facade.api.result.TrackImportLogVO;
import com.timevale.forward.facade.api.result.TrackImportProgressVO;
import com.timevale.forward.facade.api.result.TrackImportTemplateVO;
import com.timevale.forward.service.excel.track.TrackCheckListener;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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

    @Value("${templateFileId:b210b33a8167439e930dfe7dd3808ab8}")
    private String templateFileId;

    // private static final String FILE_NAME = "E:\\test.xlsx";

    @Override
    public BaseResult<Boolean> importEvent(TrackImportReq trackImportReq) {
        try {
            // 获取文件流
            InputStream in = getFileInputStream(trackImportReq.getFileId());

            // 校验文件类型
            boolean isXlsx = ExcelFileUtil.isXlsx(in);
            AssertUtil.checkState(isXlsx, "导入文件仅支持xls和xlsx格式");

            // 校验表头及事件数
            EasyExcel.read(in, TrackRow.class, new TrackCheckListener())
                    .sheet()
                    .headRowNumber(3)
                    .doRead();

            // 校验内容及格式
            EasyExcel.read(in, TrackRow.class, new TrackListener())
                    .sheet()
                    .headRowNumber(3)
                    .doRead();
            // 关闭文件流
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
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

}
