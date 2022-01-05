package com.timevale.forward.service.component;

import com.timevale.forward.facade.api.result.BizDemandStatusVO;
import com.timevale.security.facade.response.GroupResponse;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author by YangXu
 * @date 2022/01/04 10:26
 */
public interface BizDemandComponent {

    /**
     * 更新业务需求状态根据关联的产品需求
     *
     * @param bizDemandId 业务需求id
     * @return 业务需求状态VO
     */
    BizDemandStatusVO updateBizDemandStatusAsLinkProductDemand(Long bizDemandId);

    /**
     * 深搜部门树
     * 获取所有子部门及其完整链名
     * 传入的Map储存结果
     *
     * @param node           节点
     * @param deptMap        部门信息id和名称的映射
     * @param queryDeptIdSet 包含的id
     * @param name           部门完整名称
     * @param isInsert       判断是否可直接插入
     */
    void dfsGroupListTree(GroupResponse node, Map<Long, String> deptMap, Set<Long> queryDeptIdSet, String name, Boolean isInsert);


    /**
     * 得到业务需求，关联的产品需求，关联的项目的发布时间
     *
     * @param bizDemandId 业务需求id
     * @return 项目发布时间
     */
    Date getProjectEndDate(Long bizDemandId);
}
