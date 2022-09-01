package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizLabelQueryList;
import com.timevale.forward.facade.api.request.BizLabelAddListReq;
import com.timevale.forward.facade.api.request.BizLabelAddReq;
import com.timevale.forward.facade.api.result.LabelDetailVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BizLabelService {



    /**
     * 新增
     *
     * @param bizLabelAddReq bizLabelAddReq
     * @return Boolean
     */
    BaseResult<Boolean> markOrUnMark(BizLabelAddReq bizLabelAddReq);

    /**
     * 批量新增
     * @param req
     * @return
     */
    BaseResult<Boolean> batchAddLabels(BizLabelAddListReq req);

    /**
     * 已选中标签id
     * @param bizLabelQueryList bizLabelQueryList
     * @return 标签id
     */
    BaseResult<List<LabelDetailVO>> getSelectedLabel(BizLabelQueryList bizLabelQueryList);
}
