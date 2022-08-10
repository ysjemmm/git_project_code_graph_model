package com.timevale.forward.service.integration.inneruser;

import com.timevale.security.facade.response.RoleResponse;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/08/03 15:28
 */
public interface InnerUserPermissionClient {
    /**
     * 查询用户所有功能角色
     *
     * @param userId 用户id
     * @return List<RoleResponse>
     */
    List<RoleResponse> getFunctionRoleInfo(String userId);
}
