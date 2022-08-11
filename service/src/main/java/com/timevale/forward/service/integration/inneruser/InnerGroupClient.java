package com.timevale.forward.service.integration.inneruser;

import com.timevale.security.facade.response.GroupResponse;
import com.timevale.security.facade.response.SimpleGroupResponse;

import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2021/12/27 14:05
 */
public interface InnerGroupClient {

    /**
     * 批处理得到部门简单信息
     *
     * @param deptIdList 部门id列表
     * @return 部门简单信息
     */
    List<SimpleGroupResponse> batchGetSimpleGroupList(List<String> deptIdList);

    /**
     * 批处理得到部门id名称映射
     *
     * @param deptIdList 部门id列表
     * @return 部门id，名称Map
     */
    Map<String, String> batchGetSimpleGroupMap(List<String> deptIdList);

    /**
     * 得到单个部门信息
     *
     * @param deptId 部门id
     * @return 部门简单信息
     */
    SimpleGroupResponse getSimpleGroup(Long deptId);

    /**
     * 获取部门链（只包含自身及其上级部门）
     *
     * @param deptId 部门id
     * @return 部门链信息列表
     */
    List<GroupResponse> getGroupChain(Long deptId);

    /**
     * 获取子部门列表 (不包含自身)
     *
     * @param deptId 部门id
     * @return 部门信息列表
     */
    List<SimpleGroupResponse> getAllSubSimpleGroupList(Long deptId);


    /**
     * 得到部门树
     *
     * @param isTree 是树
     * @return 部门树
     */
    List<GroupResponse> getGroupListTree(Boolean isTree);
}
