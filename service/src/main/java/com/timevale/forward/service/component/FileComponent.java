package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;

import java.util.List;

public interface FileComponent {
    /**
     *
     * @param list 附件列表
     * @param attacheId 附属id
     * @param type 附属类型
     */
    void add(List<FileAddReq> list,Long attacheId,Byte type);

    /**
     *
     * @param attacheId 附属id
     * @param type 附属类型
     * @return 列表
     */
    List<FileDO> select(Long attacheId, Byte type);
}
