package com.timevale.forward.service.integration.inneruser;

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
    List<SimpleGroupResponse> batchGetSimpleGroupList(List<Long> deptIdList);

    /**
     * 批处理得到部门id名称映射
     *
     * @param deptIdList 部门id列表
     * @return 部门id，名称Map
     */
    Map<Long, String> batchGetSimpleGroupMap(List<Long> deptIdList);

    /**
     * 得到单个部门信息
     *
     * @param deptId 部门id
     * @return {@link SimpleGroupResponse }
     */
    SimpleGroupResponse getSimpleGroup(Long deptId);
}
