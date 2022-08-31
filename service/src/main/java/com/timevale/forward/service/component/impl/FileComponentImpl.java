package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    @Override
    public void add(List<FileAddReq> list, Long attacheId, Integer type) {
        log.info("新增时,附件接收参数:list={},attacheId={},type={}", list,attacheId,type);
        if(CollectionUtils.isEmpty(list)){
            return;
        }
        List<FileDO> existFiles = fileMapper.select(attacheId, type);
        log.info("已存在附件:existPersons={}", existFiles);
        if(CollectionUtils.isEmpty(existFiles)){
            List<FileDO> fileDO = FileCopier.INSTANCE.convert(list);
            fileDO.forEach(f-> fillInfo(f,attacheId,type));
            fileMapper.inserts(fileDO);

//            List<String> fileNameList = existFiles.stream()
//                    .map(file -> file.getFileName())
//                    .collect(Collectors.toList());
//
//            addLog(fileNameList, attacheId, ButtonActionEnum.LINK, type);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(List<FileAddReq> list, Long attacheId, Integer type) {
        log.info("编辑时,附件接收参数:list={},attacheId={},type={}", list,attacheId,type);
        if(CollectionUtils.isEmpty(list)){
            // 删除
            FileDO fileDO=new FileDO();
            fileDO.setIsDeleted(true);
            fileDO.setAttacheId(attacheId);
            fileDO.setType(type);
            fileMapper.update(fileDO);
            return;
        }
        // 差异对比
        List<FileDO> existFiles = fileMapper.select(attacheId, type);
        log.info("已存在附件:existFiles={}", existFiles);
        List<FileDO> fileDO = FileCopier.INSTANCE.convert(list);
        fileDO.forEach(f-> fillInfo(f,attacheId,type));
        List<String> existFileIds = existFiles.stream().map(FileDO::getFileId).collect(Collectors.toList());
        List<FileDO> needAddFiles=new ArrayList<>();
        fileDO.forEach((f)->{
            if(!existFileIds.contains(f.getFileId())){
                needAddFiles.add(f);
            }else{
                // 更新已存在的附件
                FileDO updateFileDO = new FileDO();
                updateFileDO.setType(type);
                updateFileDO.setAttacheId(attacheId);
                updateFileDO.setFileId(f.getFileId());
                updateFileDO.setFileName(f.getFileName());
                fileMapper.update(updateFileDO);
            }
        });
        if(CollectionUtils.isNotEmpty(needAddFiles)){
            fileMapper.inserts(needAddFiles);
            log.info("编辑时,新增附件:needAddFiles={},type={}", needAddFiles,type);

//            List<String> addFileNameList = needAddFiles.stream()
//                    .map(FileDO::getFileName)
//                    .collect(Collectors.toList());
//
//            addLog(addFileNameList, attacheId, ButtonActionEnum.LINK, type);
        }
        List<String> reqFileIds = fileDO.stream().map(FileDO::getFileId).collect(Collectors.toList());
//        List<String> needDeleteList = Lists.newArrayList();
        existFiles.forEach((f)->{
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            if(!reqFileIds.contains(f.getFileId())){
                f.setIsDeleted(true);
                f.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
                f.setModifyManId(userInfo.getId());
                //删除
                fileMapper.update(f);

//                needDeleteList.add(f.getFileName());
            }
        });

//        if (CollectionUtils.isNotEmpty(needDeleteList)) {
//            addLog(needDeleteList, attacheId, ButtonActionEnum.UN_LINK, type);
//        }
    }

    @Override
    public List<FileDO> select(Long attacheId, Integer type) {
        return fileMapper.select(attacheId, type);
    }

    @Override
    public List<FileDO> select(Collection<Long> attacheIdList, Integer type) {
        return fileMapper.selectByAttacheIdList(attacheIdList, type);
    }

//    private void addLog(List<String> fileNameList, Long id, ButtonActionEnum actionEnum, Integer type) {
//        if (!FileTypeEnum.BIZ_DEMAND.getCode().equals(type)) {
//            return;
//        }
//
//        for (String fileName : fileNameList) {
//            String showName = BizChangeLogFieldEnum.ATTACHMENT.getText() +
//                    CommonConstant.WIDE_COLON + fileName;
//
//            // 日志, 状态改为待评估
//            bizDemandLogComponent.addLogWhenModifyData(fileName, fileName, id,
//                    BizChangeLogFieldEnum.ATTACHMENT.getText(), true, actionEnum.getText(), showName);
//        }
//    }

    private void fillInfo(FileDO fileDO,Long attacheId, Integer type) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        fileDO.setAttacheId(attacheId);
        fileDO.setType(type);
        fileDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        fileDO.setCreateManId(userInfo.getId());
    }
}
