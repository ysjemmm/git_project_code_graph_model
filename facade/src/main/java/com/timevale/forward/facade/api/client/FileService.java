package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.FileUploadReq;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface FileService {
    /**
     * 上传文件
     *
     * @param fileUploadReq
     * @return 文件信息
     */
    BaseResult<FileVO> upload(FileUploadReq fileUploadReq);

    /**
     * 查列表
     *
     * @param attachId
     * @return 列表
     */
    BaseResult<List<FileVO>> list(String attachId);

    /**
     * 删除附件
     *
     * @param attachId
     * @return 数量
     */
    BaseResult<Integer> delete(String attachId);
}
