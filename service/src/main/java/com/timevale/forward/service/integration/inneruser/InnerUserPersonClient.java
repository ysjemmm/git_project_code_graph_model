package com.timevale.forward.service.integration.inneruser;

import com.timevale.security.facade.response.BaseInfoResponse;

import java.util.List;

/**
 * 内部用户中心接口
 *
 * @author yuankai
 * @date 2020/11/5 16:17
 */
public interface InnerUserPersonClient {

    /**
     * 获取所有下属
     *
     * @param account 当前用户花名拼音
     * @return 下属花名拼音列表
     */
    List<String> getAllMyStaff(String account);

    /**
     * 获取所有下属（包含自己）
     *
     * @param account 当前用户花名拼音
     * @return 下属花名拼音列表（包含自己）
     */
    List<String> getAllMyStaffWithSelf(String account);

    /**
     * 获取用户
     *
     * @param accountId id
     * @return 用户
     */
    BaseInfoResponse getPersonByAccountNew(String accountId);
}
