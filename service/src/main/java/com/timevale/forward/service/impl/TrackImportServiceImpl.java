package com.timevale.forward.service.impl;

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
import com.timevale.forward.model.enums.TrackImportLogResultEnum;
import com.timevale.forward.model.enums.TrackImportLogStatusEnum;
import com.timevale.forward.service.component.TrackImportComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackImportLogCopier;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
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
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String userId = userInfo.getId();

        // 判断当前是否有导入任务
        Integer progress = trackImportComponent.getProgress(userId);
        AssertUtil.checkState(progress == null, "当前已有导入任务");

        // 初始化状态
        trackImportComponent.deleteAllStatus(userId);

        // 开始导入
        trackImportComponent.setProgress(0,userId);
        trackImportComponent.setImportResult(TrackImportLogResultEnum.LOADING.getCode(), userId);
        trackImportComponent.importEvent(trackImportReq, userInfo);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> cancel() {
        String userId = LocalSessionUtils.getUserInfo().getId();
        Integer progress = trackImportComponent.getProgress(userId);
        AssertUtil.checkState(progress != null, "取消导入失败，没有正在进行中的导入任务");

        trackImportComponent.setCancelTag(userId);
        trackImportComponent.deleteImportStatus(userId);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> cleanImportStatus(String userId) {
        trackImportComponent.deleteAllStatus(userId);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<String> getAllImportStatus(String userId) {
        Integer progress = trackImportComponent.getProgress(userId);
        Boolean cancelTag = trackImportComponent.getCancelTag(userId);
        Integer importResult = trackImportComponent.getImportResult(userId);
        String s = "progress：" + progress + ", cancelTag：" + cancelTag + ", importResult：" + importResult;
        return BaseResult.success(s);
    }

    @Override
    public BaseResult<TrackImportProgressVO> progress() {
        String userId = LocalSessionUtils.getUserInfo().getId();
        TrackImportProgressVO trackImportProgressVO = new TrackImportProgressVO();

        Integer progress = trackImportComponent.getProgress(userId);
        if (progress != null) {
            trackImportProgressVO.setProgress(progress);
            trackImportProgressVO.setResult(TrackImportLogResultEnum.LOADING.getCode());
            return BaseResult.success(trackImportProgressVO);
        }

        Integer result = trackImportComponent.getImportResult(userId);
        if (result != null) {
            // 删除过气状态
            trackImportComponent.deleteAllStatus(userId);
            TrackImportLogDO trackImportLogDO = trackImportLogMapper.selectCreateLatest(userId);
            trackImportProgressVO = TrackImportLogCopier.INSTANCE.transfer(trackImportLogDO);

            // 失败文件信息
            if (TrackImportLogResultEnum.FAILURE.getCode().equals(trackImportLogDO.getResult())) {
                FileDownloadDTO fileDownloadInfo = FileUtil.getFileDownloadInfo(trackImportLogDO.getFileId(), envUtils.getEnv());
                TrackImportLogFileVO fileVO = TrackImportLogCopier.INSTANCE.convert(fileDownloadInfo);
                trackImportProgressVO.setImportLogFileVO(fileVO);
            }
            return BaseResult.success(trackImportProgressVO);
        }

        // 无导入事件
        trackImportProgressVO.setResult(TrackImportLogResultEnum.NOTING.getCode());
        return BaseResult.success(trackImportProgressVO);
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
            e.setStatusName(TrackImportLogStatusEnum.getTextByCode(e.getStatus()));
            e.setResultName(TrackImportLogResultEnum.getTextByCode(e.getResult()));
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
