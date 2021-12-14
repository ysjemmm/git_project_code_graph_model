package com.timevale.forward.service.impl;

import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.FileService;
import com.timevale.forward.facade.api.request.FileUploadReq;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@Slf4j
@RestService
public class FileServiceImpl implements FileService {

    @Resource
    private EnvUtils envUtils;

    @Override
    public BaseResult<FileVO> upload(FileUploadReq fileUploadReq) {
//        FileDownloadDTO fileDownloadDTO = FileUtil.uploadFileToOSS(fileUploadReq.getMultipartFile(), envUtils.getEnv());
        log.info("上传文件接收参数:{}", fileUploadReq);
        return BaseResult.success(new FileVO());
    }

    @Override
    public BaseResult<List<FileVO>> list(String attachId) {
        log.info("文件列表接收参数:{}", attachId);
        return BaseResult.success(Lists.newArrayList(new FileVO()));
    }

    @Override
    public BaseResult<Integer> delete(String attachId) {
        log.info("删除文件接收参数:{}", attachId);
        return BaseResult.success(1);
    }
}
