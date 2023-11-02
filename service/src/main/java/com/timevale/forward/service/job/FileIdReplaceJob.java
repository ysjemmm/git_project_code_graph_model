package com.timevale.forward.service.job;

import cn.hutool.core.util.StrUtil;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@JobHandler(value = "fileIdReplaceJob")
public class FileIdReplaceJob extends IJobHandler {
    private final EnvUtils envUtils;
    private final FileMapper fileMapper;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        List<FileDO> files = fileMapper.selectAll();
        for (FileDO file : files) {
            if (!file.getFileId().contains("-")) {
                FileDownloadDTO info = FileUtil.getFileDownloadInfo(file.getFileId(), envUtils.getEnv());
                String fileKey = Optional.ofNullable(info)
                        .map(FileDownloadDTO::getFileKey)
                        .orElse("");
                if (StrUtil.isNotEmpty(fileKey)) {
                    fileMapper.updateFileId(file.getId(), fileKey);
                }
            }
        }

        return ReturnT.SUCCESS;
    }
}
