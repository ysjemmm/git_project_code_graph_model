package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.BizDomainGroupMatchQueryList;
import com.timevale.forward.facade.api.query.BizDomainGroupQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BizDomainGroupVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author by qiyuan
 * @date 2025/08/25 17:02
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BizDomainGroupService {

    /**
     * 列表
     *
     * @return 返回业务域集列表
     */
    BaseResult<List<BizDomainGroupVO>> bizDomainGroupList();

    /**
     * 列表
     *
     * @return 返回业务域集列表
     */
    BaseResult<PageQueryResult<BizDomainGroupVO>> bizDomainGroupList(BizDomainGroupQueryList bizDomainGroupQueryList);

    /**
     * 列表
     *
     * @return 返回业务域集列表
     */
    BaseResult<PageQueryResult<BizDomainGroupVO>> matchBizDomainGroupList(BizDomainGroupMatchQueryList bizDomainGroupMatchQueryList);

    /**
     * 新增
     *
     * @param bizDomainGroupAddReq 业务域集新增请求
     * @return Boolean
     */
    BaseResult<Boolean> add(BizDomainGroupAddReq bizDomainGroupAddReq);

    /**
     * 修改
     *
     * @param bizDomainGroupModifyReq 业务域集修改请求
     * @return Boolean
     */
    BaseResult<Boolean> update(BizDomainGroupModifyReq bizDomainGroupModifyReq);

    /**
     * 修改业务域集上架状态
     *
     * @param bizDomainGroupId 业务域集ID
     */
    BaseResult<Boolean> updateBizDomainGroupListingStatus(Long bizDomainGroupId);

    /**
     * 删除业务域
     *
     * @param bizDomainGroupId 业务域集ID
     */
    BaseResult<Boolean> deleteBizDomainGroup(Long bizDomainGroupId);

    /**
     * 产品线列表
     *
     * @return 返回业务域集的产品线列表
     */
    BaseResult<List<ProductLineVO>> productLineList(Long bizDomainGroupId);

    /**
     * 查询有分组的业务域集列表
     *
     * @param bizDomainGroupQueryList 查询条件
     * @return 返回有分组的业务域集列表
     */
    BaseResult<PageQueryResult<BizDomainGroupVO>> bizDomainGroupListWithGroups(BizDomainGroupQueryList bizDomainGroupQueryList);
}
