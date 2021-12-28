package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizDemandLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandProductDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandLinkProductDemandReq;
import com.timevale.forward.facade.api.request.BizDemandUnlinkProductDemandReq;
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
     * @return 分页数据
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
     * 关联/取消关联产品需求
     *
     * @param bizDemandLinkProductDemandReq 业务需求链接产品需求
     * @return 成功与否
     */
    BaseResult<Boolean> linkProductDemand(BizDemandLinkProductDemandReq bizDemandLinkProductDemandReq);

    /**
     * 取消关联对应产品需求
     *
     * @param bizDemandUnlinkProductDemandReq 业务需求拆开产品需求要求的事情
     * @return 成功与否
     */
    BaseResult<Boolean> unlinkProductDemand(BizDemandUnlinkProductDemandReq bizDemandUnlinkProductDemandReq);

    /**
     * 产品需求列表
     *
     * @param bizDemandSubProductDemandQueryList 业务需求子产品需求查询列表
     * @return 列表
     */
    BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> matchProductDemandList(BizDemandLinkProductDemandQueryList bizDemandSubProductDemandQueryList);
}
