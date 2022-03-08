package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.client.FileService;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class FileServiceImpl implements FileService {
    @Resource
    private FileComponent fileComponent;

    @Resource
    private FileMapper fileMapper;

    @Override
    public BaseResult<Boolean> add(FileAddReq fileAddReq) {
        // 抄送人
        List<FileDO> list = fileComponent.select(0L, fileAddReq.getType());
        if(CollectionUtils.isEmpty(list)){
            fileComponent.add(Lists.newArrayList(fileAddReq),0L,fileAddReq.getType());
        }else{
            FileDO fileDO=new FileDO();
            fileDO.setFileId(fileAddReq.getFileId());
            fileDO.setId(list.get(0).getId());
            fileMapper.updateFileId(fileDO);
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<FileVO>> getFiles() {
        // 获取页面图片信息,非附件
        List<FileDO> list = fileComponent.select(0L, null);
        return BaseResult.success(FileCopier.INSTANCE.transform(list));
    }

}
