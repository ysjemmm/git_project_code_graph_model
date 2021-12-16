package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.service.component.FileComponent;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-16 11:37
 **/
@Component
public class FileComponentImpl implements FileComponent {

//    @Resource
    private FileMapper fileMapper;
    
    @Override
    public Integer add(List<FileAddReq> files) {
        return null;
    }

    @Override
    public List<FileDO> select(Long attacheId, Byte type) {
        return null;
    }
}
