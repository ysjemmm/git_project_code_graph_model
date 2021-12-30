package com.timevale.forward.service.integration.inneruser.impl;

import com.google.common.collect.Lists;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.security.facade.api.RpcGroupService;
import com.timevale.security.facade.request.GroupRequest;
import com.timevale.security.facade.response.GroupResponse;
import com.timevale.security.facade.response.SimpleGroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/27 14:07
 */
@Slf4j
@Component
public class InnerGroupClientImpl implements InnerGroupClient {

    @Resource
    RpcGroupService rpcGroupService;

    @Override
    public List<SimpleGroupResponse>batchGetSimpleGroupList(List<Long> deptIdList) {
        try{
            List<String> deptIdStringList = deptIdList.stream().distinct().map(String::valueOf).collect(Collectors.toList());
            BaseResult<List<SimpleGroupResponse>> listBaseResult = rpcGroupService.batchGetSimpleGroupList(deptIdStringList);
            if(listBaseResult.ifSuccess()){
                return listBaseResult.getData();
            }
            log.error("[innerGroup]调用内部部门中心失败  error: " + listBaseResult.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("调用内部部门中心失败  error: " + e.getMessage(), e);
            throw new BaseBizRuntimeException("调用内部部门中心失败! " + deptIdList);
        }
    }

    @Override
    public Map<Long, String> batchGetSimpleGroupMap(List<Long> deptIdList) {
        List<SimpleGroupResponse> groupList = batchGetSimpleGroupList(deptIdList);
        Map<Long, String> result = new HashMap<>(groupList.size());
        groupList.forEach(iter -> result.put(Long.parseLong(iter.getGroupId()), iter.getGroupName()));
        return result;
    }

    @Override
    public SimpleGroupResponse getSimpleGroup(Long deptId) {
        return batchGetSimpleGroupList(Lists.newArrayList(deptId)).get(0);
    }

    @Override
    public List<GroupResponse> getGroupTree(Long deptId){
        try{
            GroupRequest groupRequest = new GroupRequest();
            groupRequest.setGroupId(deptId.toString());
            BaseResult<List<GroupResponse>> groupTree = rpcGroupService.getGroupTree(groupRequest);
            if(groupTree.ifSuccess()){
                return groupTree.getData();
            }
            log.error("[innerGroup]调用内部部门中心失败  error: " + groupTree.getMessage());
            return new ArrayList<>();
        }catch (Exception e) {
            log.error("调用内部部门中心失败  error: " + e.getMessage(), e);
            throw new BaseBizRuntimeException("调用内部部门中心失败! " + deptId);
        }
    }

    @Override
    public List<SimpleGroupResponse> getAllSubSimpleGroupList(Long deptId){
        try {
            BaseResult<List<SimpleGroupResponse>> allSubSimpleGroupList = rpcGroupService.getAllSubSimpleGroupList(deptId.toString());
            if(allSubSimpleGroupList.ifSuccess()){
                return allSubSimpleGroupList.getData();
            }
            log.error("[innerGroup]调用内部部门中心失败  error: " + allSubSimpleGroupList.getMessage());
            return new ArrayList<>();
        }catch (Exception e) {
            log.error("调用内部部门中心失败  error: " + e.getMessage(), e);
            throw new BaseBizRuntimeException("调用内部部门中心失败! " + deptId);
        }
    }

    @Override
    public GroupResponse getGroupListTree(Boolean isTree) {
        try{
            BaseResult<List<GroupResponse>> groupListTree = rpcGroupService.getGroupListTree(true);
            if(groupListTree.ifSuccess()){
                return groupListTree.getData().get(0);
            }
            log.error("[innerGroup]调用内部部门中心失败  error: " + groupListTree.getMessage());
            return new GroupResponse();
        }catch (Exception e){
            log.error("调用内部部门中心失败  error: " + e.getMessage(), e);
            throw new BaseBizRuntimeException("调用内部部门中心失败! ");
        }
    }

}
