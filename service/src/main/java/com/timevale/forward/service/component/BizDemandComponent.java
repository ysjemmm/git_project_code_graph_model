package com.timevale.forward.service.component;

import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.security.facade.response.GroupResponse;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2022/01/04 10:26
 */
public interface BizDemandComponent {

    /**
     * 刷新业务需求状态
     *
     * @param bdId 业务需求id
     */
    void updateStatus(Long bdId);

    /**
     * 获取所有子部门及其完整链名
     *
     * @param queryDeptIds 查询部门id列表
     * @return 部门id -> 部门信息（完整名称）
     */
    Map<Long, GroupResponse> getGroupListTreeMap(Collection<Long> queryDeptIds);

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
    QueryResultVO<BizDemandVO> page(BizDemandListCondition bizDemandListCondition);

    /**
     * 转交
     *
     * @param id              id
     * @param newReceiveMan   新接收人
     * @param newReceiveManId 新接收人身份证
     * @return {@link Boolean}
     */
    Boolean transfer(Long id, String newReceiveMan, String newReceiveManId);

    /**
     *
     * @param bizDemandId bizDemandIds
     */
    void updateProjectEndDate(Long bizDemandId);

    /**
     *
     * @param pdStauts pdStauts
     */
    Integer getBizDemandStatus(Integer pdStauts);

    /**
     * 更新项目，判断是否客开项目
     *
     * @param bdId bd id
     */
    void updateCustomerPj(Long bdId);

    List<Long> getLinkProjectIds(Long bizDemandId);

    /**
     * 查询关联的产品需求
     *
     * @param bdId 业务需求id
     * @return 产品需求DO
     */
    List<ProductDemandDO> getPdDO(Long bdId);

    /**
     * 计算指定业务需求的状态
     *
     * @param bdId 业务需求id
     * @return 返回业务需求状态
     */
    Integer getStatus(Long bdId);
}
