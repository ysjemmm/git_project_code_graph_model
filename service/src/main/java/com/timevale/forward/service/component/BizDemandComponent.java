package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2022/01/04 10:26
 */
public interface BizDemandComponent {

    /**
     * 更新业务需求状态根据关联的产品需求
     *
     * @param bizDemandId 业务需求id
     */
    void updateBizDemandStatusByLinkedProductDemand(Long bizDemandId);

    /**
     * 获取所有子部门及其完整链名
     *
     * @param queryDeptIdList 查询部门id列表
     * @return 部门id -> 部门信息（完整名称）
     */
    Map<Long, GroupResponse> getGroupListTreeMap(List<Long> queryDeptIdList);

    /**
     * 获取部门完整链名
     *
     * @param deptId 部门id
     * @return 完整链名
     */
    String getDeptChainName(Long deptId);

    /**
     * 得到业务需求，关联的产品需求，关联的项目的发布时间
     *
     * @param bizDemandId 业务需求id
     * @return 项目发布时间
     */
    Date getProjectEndDate(Long bizDemandId);

    /**
     * 列表
     *
     * @param bizDemandListCondition 业务需求查询列表
     * @return 列表
     */
    BaseResult<PageQueryResult<BizDemandVO>> page(BizDemandListCondition bizDemandListCondition);

    /**
     *
     * @param bizDemandIds bizDemandIds
     */
    void updateProjectEndDate(List<Long> bizDemandIds);
}
