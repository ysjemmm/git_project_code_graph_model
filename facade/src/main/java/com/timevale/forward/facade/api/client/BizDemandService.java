package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandAddReq;
import com.timevale.forward.facade.api.request.BizDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author by YangXu
 * @Date 2021/12/14 14:03
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BizDemandService {

    /**
     * 列表
     *
     * @param bizDemandQueryList 业务需求查询列表
     * @return 列表
     */
    BaseResult<PageQueryResult<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList);

    /**
     * 通过id获取业务需求
     *
     * @param bizDemandId 业务需求id
     * @return 单个业务需求详情
     */
    BaseResult<BizDemandVO> getBizDemandById(Long bizDemandId);

    /**
     * 添加业务需求
     *
     * @param bizDemandAddReq 业务需求添加请求
     * @return 成功与否
     */
    BaseResult<Boolean> addBizDemand(BizDemandAddReq bizDemandAddReq);

    /**
     * 修改业务需求
     *
     * @param bizDemandModifyReq 业务需求修改请求
     * @return 成功与否
     */
    BaseResult<Boolean> modifyBizDemand(BizDemandModifyReq bizDemandModifyReq);


    /**
     * 同意接收
     *
     * @param bizDemandId     业务需求id
     * @param planReleaseDate 预期上线时间
     * @return 成功与否
     */
    BaseResult<Boolean> agree(Long bizDemandId, Integer planReleaseDate);

    /**
     * 驳回
     *
     * @param bizDemandId 业务需求id
     * @param reason      驳回理由
     * @return
     */
    BaseResult<Boolean> reject(Long bizDemandId, Integer reason);
}
