package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizDomainQueryList;
import com.timevale.forward.facade.api.request.BizDomainAddReq;
import com.timevale.forward.facade.api.request.BizDomainModifyReq;
import com.timevale.forward.facade.api.request.UpdateBizDomainListingStatusReq;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/13 17:02
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BizDomainService {

    /**
     * 列表
     *
     * @return 返回业务域列表
     */
    BaseResult<List<BizDomainVO>> bizDomainList();

    /**
     * 列表
     *
     * @return 返回业务域列表
     */
    BaseResult<PageQueryResult<BizDomainVO>> bizDomainList(BizDomainQueryList bizDomainQueryList);

    /**
     * 新增
     *
     * @param bizDomainAddReq 业务域新增请求
     * @return Boolean
     */
    BaseResult<Boolean> add(BizDomainAddReq bizDomainAddReq);

    /**
     * 修改
     *
     * @param bizDomainModifyReq 业务域修改请求
     * @return Boolean
     */
    BaseResult<Boolean> update(BizDomainModifyReq bizDomainModifyReq);

    /**
     * 删除
     *
     * @param bizDomainId 业务域删除请求
     * @return Boolean
     */
    @Deprecated
    BaseResult<Boolean> delOrUnDelete(Long bizDomainId);

    /**
     * 修改业务域上架状态
     *
     * @param req 请求对象
     */
    BaseResult<Boolean> updateBizDomainListingStatus(UpdateBizDomainListingStatusReq req);

    /**
     * 删除业务域
     *
     * @param bizDomainId 业务域ID
     */
    BaseResult<Boolean> deleteBizDomain(Long bizDomainId);

}
