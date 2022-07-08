package com.timevale.forward.service.integration.inneruser.impl;

import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.security.facade.api.RpcPersonService;
import com.timevale.security.facade.request.AccountRequest;
import com.timevale.security.facade.request.GroupRequest;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Override
    public Set<String> getAllSuperiorByAccount(String userId, boolean isLeave) {
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccount(userId);
        accountRequest.setIsLeave(isLeave);
        BaseResult<Set<String>> res = getAllSuperiorByAccount(accountRequest);
        if (res.ifSuccess()) {
            return res.getData();
        } else {
            throw new BaseBizRuntimeException("调用内部用户中心查询上级失败! " + userId);
        }
    }

    @Override
    public BaseResult<Set<String>> getAllSuperiorByAccount(AccountRequest request) {
        return rpcPersonService.getAllSuperiorByAccount(request);
    }

    /**
     * 获取所有下属
     *
     * @param account 当前用户花名拼音
     * @return 下属花名拼音列表
     */
    @Override
    public List<String> getAllMyStaff(String account, Boolean isLeave) {
        try {
            final AccountRequest request = new AccountRequest();
            request.setAccount(account);
            request.setIsLeave(isLeave);
            final BaseResult<Set<String>> allMyStaffNew = rpcPersonService.getAllMyStaffNew(request);
            if (allMyStaffNew.ifSuccess()) {
                return new ArrayList<>(allMyStaffNew.getData());
            }
            log.error("[innerUser]调用内部用户中心失败 getAllMyStaffNew account: " + account + " error: " + allMyStaffNew.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getAllMyStaffNew account: " + account + " error: " + e.getMessage(), e);
            throw new BaseBizRuntimeException("调用内部用户中心失败! " + account);
        }
    }

    @Override
    public List<BaseInfoResponse> getAllMyStaffInfo(String account, Boolean isLeave) {
        try {
            final AccountRequest request = new AccountRequest();
            request.setAccount(account);
            request.setIsLeave(isLeave);
            final BaseResult<List<BaseInfoResponse>> allMyStaffs = rpcPersonService.getAllMyStaffs(request);
            if (allMyStaffs.ifSuccess()) {
                return new ArrayList<>(allMyStaffs.getData());
            }
            log.error("[innerUser]调用内部用户中心失败 getAllMyStaffs account: " + account + " error: " + allMyStaffs.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getAllMyStaffs account: " + account + " error: " + e.getMessage(), e);
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
    public List<String> getAllMyStaffWithSelf(String account, Boolean isLeave) {
        List<String> allMyStaff = this.getAllMyStaff(account, isLeave);
        allMyStaff.add(account);
        return allMyStaff;
    }

    @Override
    public List<BaseInfoResponse> getAllMyStaffWithSelfInfo(String account, Boolean isLeave) {
        try {
            final AccountRequest request = new AccountRequest();
            request.setAccount(account);
            request.setIsLeave(isLeave);
            final BaseResult<BaseInfoResponse> accountInfo = rpcPersonService.getByAccount(request);
            if (accountInfo.ifSuccess()) {
                List<BaseInfoResponse> allMyStaffInfoWithSelf = this.getAllMyStaffInfo(account, isLeave);
                allMyStaffInfoWithSelf.add(accountInfo.getData());
                return allMyStaffInfoWithSelf;
            }
            log.error("调用内部用户中心失败 getByAccount account: " + account + " error: " + accountInfo);
            return Lists.emptyList();
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getByAccount account: " + account + " error: " + e.getMessage(), e);
        }
        throw new BaseBizRuntimeException("调用内部用户中心失败! " + account);
    }

    @Override
    public List<BaseInfoResponse> getPersonByAccountNew(List<String> accountIds) {
        try {
            BaseResult<List<BaseInfoResponse>> personByAccountNew = rpcPersonService.getPersonByAccountNew(accountIds);
            if (personByAccountNew.ifSuccess() && !CollectionUtils.isEmpty(personByAccountNew.getData())) {
                return personByAccountNew.getData();
            }
            log.error("调用内部用户中心失败 getPersonByAccountNew account: " + accountIds + " error: " + personByAccountNew);
            return Lists.emptyList();
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getPersonByAccountNew account: " + accountIds + " error: " + e.getMessage(), e);
        }
        throw new BaseBizRuntimeException("调用内部用户中心失败! " + accountIds);
    }

    /**
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
                personInGroup.getData().forEach(t -> accountIds.add(t.getAccount()));
                return accountIds;
            }
            log.error("调用内部用户中心失败 getAllStaffsByGroupId groupId: " + groupId + " error: " + personInGroup);
            return Lists.emptyList();
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getAllStaffsByGroupId groupId: " + groupId + " error: " + e.getMessage(), e);
        }
        throw new BaseBizRuntimeException("调用内部用户中心失败! " + groupId);
    }

    /**
     * 获取用户
     *
     * @param accountIds id
     * @return accountId-unionId
     */
    @Override
    public Map<String, String> getUnionIds(List<String> accountIds) {
        try {
            BaseResult<List<BaseInfoResponse>> personByAccountNew = rpcPersonService.getPersonByAccountNew(accountIds);
            if (personByAccountNew.ifSuccess() && !CollectionUtils.isEmpty(personByAccountNew.getData())) {
                return personByAccountNew.getData().stream().filter(a -> StringUtils.isNotEmpty(a.getUnionId()))
                        .collect(Collectors.toMap(BaseInfoResponse::getAccount, BaseInfoResponse::getUnionId, (v1, v2) -> v1));
            }
            log.error("调用内部用户中心失败 getPersonByAccountNew account: " + accountIds + " error: " + personByAccountNew);
            return Maps.newHashMap();
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
        List<String> accountIds = Lists.newArrayList();
        try {
            GroupRequest groupRequest = new GroupRequest();
            groupRequest.setGroupId(groupId);
            BaseResult<List<BaseInfoResponse>> personInGroup = rpcPersonService.getByGroupIdNew(groupRequest);
            if (personInGroup.ifSuccess()) {
                if (!CollectionUtils.isEmpty(personInGroup.getData())) {
                    personInGroup.getData().forEach(e -> accountIds.add(e.getAccount()));
                }
                return accountIds;
            }
        } catch (Exception e) {
            log.error("调用内部用户中心失败 getByGroupIdNew groupId: " + groupId + " error: " + e.getMessage(), e);
        }
        throw new BaseBizRuntimeException("调用内部用户中心失败! " + groupId);
    }
}
