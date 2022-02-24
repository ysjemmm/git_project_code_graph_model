package com.timevale.forward.service.integration.inneruser;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.security.facade.request.AccountRequest;
import com.timevale.security.facade.response.BaseInfoResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内部用户中心接口
 *
 * @author yuankai
 * @date 2020/11/5 16:17
 */
public interface InnerUserPersonClient {
    /**
     * 查询上级
     *
     * @param request 查询用户的相关信息
     * @return 上级花名拼音列表
     */
    BaseResult<Set<String>> getAllSuperiorByAccount(AccountRequest request);

    /**
     * 获取所有下属
     *
     * @param account 当前用户花名拼音
     * @param isLeave 是否包含已经离职用户
     * @return 下属花名拼音列表
     */
    List<String> getAllMyStaff(String account, Boolean isLeave);

    /**
     * 获取所有下属详细信息
     *
     * @param account 当前用户花名拼音
     * @param isLeave 是否包含已经离职用户
     * @return 下属信息列表
     */
    List<BaseInfoResponse> getAllMyStaffInfo(String account, Boolean isLeave);

    /**
     * 获取所有下属（包含自己）
     *
     * @param account 当前用户花名拼音
     * @param isLeave 是否包含离职用户
     * @return 下属花名拼音列表（包含自己）
     */
    List<String> getAllMyStaffWithSelf(String account, Boolean isLeave);

    /**
     * 获取所有下属详细信息（包含自己）
     *
     * @param account 当前用户花名拼音
     * @param isLeave 是否包含已经离职用户
     * @return 下属信息列表（包含自己）
     */
    List<BaseInfoResponse> getAllMyStaffWithSelfInfo(String account, Boolean isLeave);

    /**
     * 获取用户
     *
     * @param accountIds id
     * @return 用户
     */
    List<BaseInfoResponse> getPersonByAccountNew(List<String> accountIds);

    /**
     * 获取当前部门及其所有子部门员工 (含离职)
     *
     * @param groupId groupId
     * @return String
     */
    List<String> getAllByGroupId(String groupId);

    /**
     * 获取部门员工（不包含离职）
     *
     * @param groupId 部门id
     * @return 员工idList
     */
    List<String> getByGroupIdNew(String groupId);

    /**
     * 获取用户
     *
     * @param accountIds id
     * @return accountId-unionId
     */
    Map<String, String> getUnionIds(List<String> accountIds);
}
