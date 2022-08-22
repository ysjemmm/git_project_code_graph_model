package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.TrackImportLogMapper;
import com.timevale.forward.dal.entity.TrackImportLogDO;
import com.timevale.forward.model.enums.TrackImportLogResultEnum;
import com.timevale.forward.model.enums.TrackImportLogStatusEnum;
import com.timevale.forward.service.component.TrackImportLogComponent;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author by YangXu
 * @date 2022/08/12 15:55
 */
@Slf4j
@LogPoint
@Component
public class TrackImportLogComponentImpl implements TrackImportLogComponent {

    @Resource
    private TrackImportLogMapper trackImportLogMapper;

    @Override
    public void failLog(String failFileId, int importCount, int importFailCount, UserInfo userInfo) {
        log.info("失败导入记录");
        TrackImportLogDO trackImportLogDO = new TrackImportLogDO();
        trackImportLogDO.setImportCount(importCount);
        trackImportLogDO.setImportFailCount(importFailCount);
        trackImportLogDO.setFileId(failFileId);
        trackImportLogDO.setCreateMan(userInfo.getAlias() + "-" + userInfo.getName());
        trackImportLogDO.setCreateManId(userInfo.getId());
        trackImportLogDO.setResult(TrackImportLogResultEnum.FAILURE.getCode());
        trackImportLogDO.setStatus(TrackImportLogStatusEnum.SUCCESS.getCode());
        trackImportLogMapper.insert(trackImportLogDO);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void allFailLog(String importFileId, UserInfo userInfo) {
        log.info("完全错误记录");
        TrackImportLogDO trackImportLogDO = new TrackImportLogDO();
        trackImportLogDO.setImportCount(0);
        trackImportLogDO.setImportFailCount(0);
        trackImportLogDO.setFileId(importFileId);
        trackImportLogDO.setCreateMan(userInfo.getAlias() + "-" + userInfo.getName());
        trackImportLogDO.setCreateManId(userInfo.getId());
        trackImportLogDO.setResult(TrackImportLogResultEnum.FAILURE.getCode());
        trackImportLogDO.setStatus(TrackImportLogStatusEnum.FAILURE.getCode());
        trackImportLogMapper.insert(trackImportLogDO);
    }

    @Override
    public void successLog(String importFileId, int importCount, UserInfo userInfo) {
        log.info("成功导入记录");
        TrackImportLogDO trackImportLogDO = new TrackImportLogDO();
        trackImportLogDO.setStatus(TrackImportLogStatusEnum.SUCCESS.getCode());
        trackImportLogDO.setResult(TrackImportLogResultEnum.SUCCESS.getCode());
        trackImportLogDO.setImportCount(importCount);
        trackImportLogDO.setImportFailCount(0);
        trackImportLogDO.setFileId(importFileId);
        trackImportLogDO.setCreateMan(userInfo.getAlias() + "-" + userInfo.getName());
        trackImportLogDO.setCreateManId(userInfo.getId());
        trackImportLogMapper.insert(trackImportLogDO);
    }
}
