package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizDemandLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandProductDemandQueryList;
import com.timevale.forward.facade.api.request.LinkOrUnLinkProductDemandReq;
import com.timevale.forward.facade.api.result.BizDemandLinkProductDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author by YangXu
 * @date 2021/12/23 17:55
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BizDemandProductDemandService {

    /**
     * 关联产品需求列表
     *
     * @param bizDemandProductDemandQueryList 业务需求产品需求查询列表
     * @return {@link BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> }
     */
    BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> linkProductDemandList(BizDemandProductDemandQueryList bizDemandProductDemandQueryList);

    /**
     * 查看产品需求
     *
     * @param productDemandId 产品需求id
     * @return 产品需求详细信息
     */
    BaseResult<ProductDemandDetailVO> getProductDemand(Long productDemandId);


    /**
     * 取消关联对应产品需求
     *
     * @param bizDemandId     业务需求id
     * @param productDemandId 产品需求id
     * @return 成功与否
     */
    BaseResult<Boolean> unlinkProductDemand(Long bizDemandId, Long productDemandId);

    /**
     * 产品需求列表
     *
     * @param bizDemandSubProductDemandQueryList 业务需求子产品需求查询列表
     * @return 列表
     */
    BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> matchProductDemandList(BizDemandLinkProductDemandQueryList bizDemandSubProductDemandQueryList);

    /**
     * 关联/取消关联产品需求
     *
     * @param linkOrUnLinkProductDemandReq 关联产品线需求请求
     * @return 成功与否
     */
    BaseResult<Boolean> linkOrUnLinkProductDemand(LinkOrUnLinkProductDemandReq linkOrUnLinkProductDemandReq);
}
