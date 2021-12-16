package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.copy.FileCopier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: xingyun
 * @create: 2021-12-16 11:37
 **/
@Component
@Slf4j
public class FileComponentImpl implements FileComponent {

    @Resource
    private FileMapper fileMapper;
    
    @Override
    public void add(List<FileAddReq> list,Long attacheId,Byte type) {
        log.info("附件新增接收参数:list={},attacheId={},type={}", list,attacheId,type);
        List<FileDO> existFiles = fileMapper.select(attacheId, type);
        List<FileDO> fileDO = FileCopier.INSTANCE.convert(list);
        if(CollectionUtils.isEmpty(existFiles)){
            // 没找到附件 直接入库
            fileMapper.inserts(fileDO);
            return;
        }
        
        List<String> existFileIds = existFiles.stream().map(FileDO::getFileId).collect(Collectors.toList());
        List<FileDO> needAddFiles=new ArrayList<>();
        fileDO.forEach((f)->{
            if(!existFileIds.contains(f.getFileId())){
                needAddFiles.add(f);
            }
        });
        fileMapper.inserts(needAddFiles);
        log.info("新增附件:needAddFiles={},type={}", needAddFiles,type);

        List<String> reqFileIds = fileDO.stream().map(FileDO::getFileId).collect(Collectors.toList());
        existFiles.forEach((f)->{
            if(!reqFileIds.contains(f.getFileId())){
                f.setIsDeleted(true);
                //删除
                fileMapper.update(f);
            }
        });
    }

    @Override
    public List<FileDO> select(Long attacheId, Byte type) {
        return fileMapper.select(attacheId, type);
    }
}
