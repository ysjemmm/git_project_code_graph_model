package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;

import java.util.List;

public interface FileComponent {

    Integer add(List<FileAddReq> files);

    List<FileDO> select(Long attacheId, Byte type);
}
