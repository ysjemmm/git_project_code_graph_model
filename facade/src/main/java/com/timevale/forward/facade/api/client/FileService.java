package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface FileService {
    /**
     * 修改
     *
     * @param fileAddReq 附件
     * @return 数量
     */
    BaseResult<Boolean> add(FileAddReq fileAddReq);


    /**
     * 查看团队成员
     *
     * @return 详情信息
     */
    BaseResult<List<FileVO>> getFiles();
}
