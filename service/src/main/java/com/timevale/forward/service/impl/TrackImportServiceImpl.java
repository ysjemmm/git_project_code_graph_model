package com.timevale.forward.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.crm.sdk.common.entity.integration.dto.FileDownloadDTO;
import com.timevale.crm.sdk.common.utils.file.FileUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.TrackImportLogMapper;
import com.timevale.forward.dal.entity.TrackImportLogDO;
import com.timevale.forward.facade.api.client.TrackImportService;
import com.timevale.forward.facade.api.query.TaskImportLogQueryList;
import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.facade.api.result.TrackImportLogFileVO;
import com.timevale.forward.facade.api.result.TrackImportLogListVO;
import com.timevale.forward.facade.api.result.TrackImportProgressVO;
import com.timevale.forward.model.enums.TrackImportResultEnum;
import com.timevale.forward.model.enums.TrackImportStatusEnum;
import com.timevale.forward.service.component.TrackImportComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackImportLogCopier;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.List;
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
    private TrackImportLogMapper trackImportLogMapper;
    @Resource
    private TrackImportComponent trackImportComponent;

    @Value("${templateFileId:d3a98af8ea754d11ae27d50b563d9c1f}")
    private String templateFileId;


    @Override
    public BaseResult<Boolean> importEvent(TrackImportReq trackImportReq) {
        trackImportComponent.importEvent(trackImportReq);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> cancel() {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TrackImportProgressVO> progress() {
        TrackImportProgressVO result = new TrackImportProgressVO();

        int i = RandomUtil.randomInt(0, 3);
        result.setStatus(i);
        if (i == 0) {
            int progress = RandomUtil.randomInt(0, 100);
            result.setProgress(progress);
        } else if(i == 1) {
            int success = RandomUtil.randomInt(1, 100);
            result.setProgress(100);
            result.setImportCount(success);
            result.setImportFailCount(0);
        } else {
            int all = RandomUtil.randomInt(50, 100);
            int fail = RandomUtil.randomInt(1, all);

            result.setProgress(100);
            result.setImportCount(all);
            result.setImportFailCount(fail);
        }

        return BaseResult.success(result);
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


}
