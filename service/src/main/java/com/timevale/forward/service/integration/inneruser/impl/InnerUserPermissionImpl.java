package com.timevale.forward.service.integration.inneruser.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.integration.inneruser.InnerUserPermissionClient;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.security.facade.api.RpcPermissionService;
import com.timevale.security.facade.enums.RpcRoleTypeEnum;
import com.timevale.security.facade.request.GetAllRoleRequest;
import com.timevale.security.facade.response.RoleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/03 15:30
 */
@Slf4j
@Component
public class InnerUserPermissionImpl implements InnerUserPermissionClient {

    @Resource
    private RpcPermissionService permissionService;

    @Override
    public List<RoleResponse> getFunctionRoleInfo(String userId) {
        GetAllRoleRequest request = new GetAllRoleRequest();
        request.setAccount(userId);
        request.setRoleTypeEnum(RpcRoleTypeEnum.FUNCTION);
        BaseResult<List<RoleResponse>> res = permissionService.getAllRoleInfo(request);
        if (res.ifSuccess()) {
            return res.getData();
        } else {
            throw new BaseBizRuntimeException("查询用户角色失败! " + userId);
        }
    }
}
