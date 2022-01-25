package com.timevale.forward.service.integration.inneruser.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.security.facade.api.RpcPersonService;
import com.timevale.security.facade.request.AccountRequest;
import com.timevale.security.facade.request.GroupRequest;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yuankai
 * @date 2020/11/5 16:19
 */
@Slf4j
@Component
public class InnerUserPersonClientImpl implements InnerUserPersonClient {

    @Resource
    private RpcPersonService rpcPersonService;

    /**
     * 获取所有下属
     *
     * @param account 当前用户花名拼音
     * @return 下属花名拼音列表
     */
    @Override
    public List<String> getAllMyStaff(String account) {
        try {
            final AccountRequest request = new AccountRequest();
            request.setAccount(account);
            request.setIsLeave(true);
            final BaseResult<Set<String>> allMyStaffNew = rpcPersonService.getAllMyStaffNew(request);
            if (allMyStaffNew.ifSuccess()) {
                return new ArrayList<>(allMyStaffNew.getData());
            }
            log.error("[innerUser]调用内部用户中心失败 account: " + account + " error: " + allMyStaffNew.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("调用内部用户中心失败 account: " + account + " error: " + e.getMessage(), e);
            throw new BaseBizRuntimeException("调用内部用户中心失败! " + account);
        }
    }

    /**
     * 获取所有下属（包含自己）
     *
     * @param account 当前用户花名拼音
     * @return 下属花名拼音列表（包含自己）
     */
    @Override
    public List<String> getAllMyStaffWithSelf(String account) {
        List<String> allMyStaff = this.getAllMyStaff(account);
        allMyStaff.add(account);
        return allMyStaff;
    }

    @Override
    public BaseInfoResponse getPersonByAccountNew(String accountId) {
        try {
            BaseResult<List<BaseInfoResponse>> personByAccountNew = rpcPersonService.getPersonByAccountNew(Collections.singletonList(accountId));
            if (personByAccountNew.ifSuccess() && !CollectionUtils.isEmpty(personByAccountNew.getData())) {
                return personByAccountNew.getData().get(0);
            }
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getPersonByAccountNew account: " + accountId + " error: " + e.getMessage(), e);
        }
        throw new BaseBizRuntimeException("调用内部用户中心失败! " + accountId);
    }

    /**
     *
     * @param groupId groupId
     * @return 部门及子部门员工(含离职)
     */
    @Override
    public List<String> getAllByGroupId(String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            throw new BaseBizRuntimeException("部门id为空! " + groupId);
        }
        List<String> accountIds = new ArrayList<>();
        try {
            BaseResult<List<BaseInfoResponse>> personInGroup = rpcPersonService.getAllStaffsByGroupId(groupId);
            if (personInGroup.ifSuccess() && !CollectionUtils.isEmpty(personInGroup.getData())) {
                personInGroup.getData().forEach(t -> {
                    accountIds.add(t.getAccount());
                });
                return accountIds;
            }
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getPersonByAccountNew groupId: " + groupId + " error: " + e.getMessage(), e);
        }
        throw new BaseBizRuntimeException("调用内部用户中心失败! " + groupId);
    }

    /**
     * 获取用户
     *
     * @param accountIds id
     * @return -unionId
     */
    @Override
    public List<String> getUnionIds(List<String> accountIds) {
        try {
            BaseResult<List<BaseInfoResponse>> personByAccountNew = rpcPersonService.getPersonByAccountNew(accountIds);
            if (personByAccountNew.ifSuccess() && !CollectionUtils.isEmpty(personByAccountNew.getData())) {
                return personByAccountNew.getData().stream().map(BaseInfoResponse::getUnionId).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getPersonByAccountNew account: " + accountIds + " error: " + e.getMessage(), e);
        }
        throw new BaseBizRuntimeException("调用内部用户中心失败! " + accountIds);
    }

    @Override
    public List<String> getByGroupIdNew(String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            throw new BaseBizRuntimeException("部门id为空! " + groupId);
        }
        List<String> accountIds = new ArrayList<>();
        try {
            GroupRequest groupRequest = new GroupRequest();
            groupRequest.setGroupId(groupId);
            BaseResult<List<BaseInfoResponse>> personInGroup = rpcPersonService.getByGroupIdNew(groupRequest);
            if (personInGroup.ifSuccess() && !CollectionUtils.isEmpty(personInGroup.getData())) {
                personInGroup.getData().forEach(t -> {
                    accountIds.add(t.getAccount());
                });
                return accountIds;
            }
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getPersonByAccountNew groupId: " + groupId + " error: " + e.getMessage(), e);
        }
        throw new BaseBizRuntimeException("调用内部用户中心失败! " + groupId);
    }
}
