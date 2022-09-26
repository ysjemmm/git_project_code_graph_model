package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class FileComponentImpl implements FileComponent {

    @Resource
    private FileMapper fileMapper;

    @Resource
    private BizDemandLogComponent bizDemandLogComponent;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Override
    public void add(List<FileAddReq> list, Long attacheId, Integer type) {
        log.info("新增时,附件接收参数:list={},attacheId={},type={}", list, attacheId, type);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<FileDO> existFiles = fileMapper.select(attacheId, type);
        log.info("已存在附件:existPersons={}", existFiles);
        if (CollectionUtils.isEmpty(existFiles)) {
            List<FileDO> fileDO = FileCopier.INSTANCE.convert(list);
            fileDO.forEach(f -> fillInfo(f, attacheId, type));
            fileMapper.inserts(fileDO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(List<FileAddReq> list, Long attacheId, Integer type) {
        log.info("编辑时,附件接收参数:list={},attacheId={},type={}", list, attacheId, type);
        List<BizChangeLogDO> logDOList = new ArrayList<>();
        List<FileDO> existFiles = fileMapper.select(attacheId, type);
        if (CollectionUtils.isEmpty(list)) {
            // 删除
            FileDO fileDO = new FileDO();
            fileDO.setIsDeleted(true);
            fileDO.setAttacheId(attacheId);
            fileDO.setType(type);
            fileMapper.update(fileDO);

            existFiles.forEach(f -> {
                String oldFile = createFile(f.getFileId(), f.getFileName());
                logDOList.add(bizDemandLogComponent.buildLogWhenUpdateFiles(oldFile, oldFile, attacheId, ButtonActionEnum.DELETE.getText()));
            });
        }else{
            // 差异对比
            Map<String, String> fileMap = existFiles.stream().collect(Collectors.toMap(FileDO::getFileId, FileDO::getFileName, (v1, v2) -> v2));
            log.info("已存在附件:existFiles={}", existFiles);
            List<String> existFileIds = existFiles.stream().map(FileDO::getFileId).collect(Collectors.toList());

            List<FileDO> reqFileDO = FileCopier.INSTANCE.convert(list);
            reqFileDO.forEach(f -> fillInfo(f, attacheId, type));

            List<FileDO> needAddFiles = new ArrayList<>();
            reqFileDO.forEach((f) -> {
                String newFile = createFile(f.getFileId(), f.getFileName());
                if (!existFileIds.contains(f.getFileId())) {
                    needAddFiles.add(f);
                    logDOList.add(bizDemandLogComponent.buildLogWhenUpdateFiles(newFile, newFile, attacheId, ButtonActionEnum.ADD.getText()));
                } else if (!Objects.equals(f.getFileName(), fileMap.get(f.getFileId()))) {
                    // 更新已存在的附件名称
                    FileDO updateFileDO = new FileDO();
                    updateFileDO.setType(type);
                    updateFileDO.setAttacheId(attacheId);
                    updateFileDO.setFileId(f.getFileId());
                    updateFileDO.setFileName(f.getFileName());
                    fileMapper.update(updateFileDO);

                    String oldFile = createFile(f.getFileId(), fileMap.get(f.getFileId()));
                    logDOList.add(bizDemandLogComponent.buildLogWhenUpdateFiles(oldFile, newFile, attacheId, ButtonActionEnum.UPDATE.getText()));
                }
            });
            if (CollectionUtils.isNotEmpty(needAddFiles)) {
                fileMapper.inserts(needAddFiles);
                log.info("编辑时,新增附件:needAddFiles={},type={}", needAddFiles, type);
            }
            List<String> reqFileIds = reqFileDO.stream().map(FileDO::getFileId).collect(Collectors.toList());
            existFiles.forEach((f) -> {
                UserInfo userInfo = LocalSessionUtils.getUserInfo();
                if (!reqFileIds.contains(f.getFileId())) {
                    f.setIsDeleted(true);
                    f.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
                    f.setModifyManId(userInfo.getId());
                    //删除
                    fileMapper.update(f);

                    String oldFile = createFile(f.getFileId(), f.getFileName());
                    logDOList.add(bizDemandLogComponent.buildLogWhenUpdateFiles(oldFile, oldFile, attacheId, ButtonActionEnum.DELETE.getText()));
                }
            });
        }
        if (FileTypeEnum.BIZ_DEMAND.getCode().equals(type) && CollectionUtils.isNotEmpty(logDOList)) {
            //业务需求新增附件日志
            bizChangeLogMapper.batchInsert(logDOList);
        }
    }

    @Override
    public List<FileDO> select(Long attacheId, Integer type) {
        return fileMapper.select(attacheId, type);
    }

    @Override
    public List<FileDO> select(Collection<Long> attacheIdList, Integer type) {
        return fileMapper.selectByAttacheIdList(attacheIdList, type);
    }

    private void fillInfo(FileDO fileDO, Long attacheId, Integer type) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        fileDO.setAttacheId(attacheId);
        fileDO.setType(type);
        fileDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        fileDO.setCreateManId(userInfo.getId());
    }

    private String createFile(String fileId, String fileName) {
        FileDO fileDO = new FileDO();
        fileDO.setFileId(fileId);
        fileDO.setFileName(fileName);
        return JSONObject.toJSONString(fileDO);
    }
}
