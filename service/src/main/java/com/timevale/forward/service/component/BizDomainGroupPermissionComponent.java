package com.timevale.forward.service.component;

/**
 * 业务域集权限校验组件
 *
 * @author kiro
 */
public interface BizDomainGroupPermissionComponent {

    /**
     * 校验操作权限
     * 只有业务域集里的产品经理才能操作
     *
     * @param bizDomainGroupId 业务域集ID
     */
    void checkOperationPermission(Long bizDomainGroupId);
}
