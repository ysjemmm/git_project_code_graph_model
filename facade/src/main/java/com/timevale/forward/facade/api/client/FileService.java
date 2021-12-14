package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface FileService {
    /**
     * 新增
     *
     * @param fileAddReq
     * @return 数量
     */
    BaseResult<Integer> add(FileAddReq fileAddReq);

    /**
     * 查列表
     *
     * @param attachId
     * @return 列表
     */
    BaseResult<List<String>> list(String attachId);

    /**
     * 删除附件
     *
     * @param attachId
     * @return 数量
     */
    BaseResult<Integer> delete(String attachId);
}
