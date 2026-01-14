package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizDomainGroupMapper;
import com.timevale.forward.dal.dao.BizDomainGroupRelationMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainGroupRelationDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.service.component.BizDomainGroupPermissionComponent;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 业务域集权限校验组件实现
 *
 * @author kiro
 */
@Component
public class BizDomainGroupPermissionComponentImpl implements BizDomainGroupPermissionComponent {

    @Resource
    private BizDomainGroupMapper bizDomainGroupMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private BizDomainGroupRelationMapper bizDomainGroupRelationMapper;

    @Override
    public void checkOperationPermission(Long bizDomainGroupId) {
        // 只有业务域集里的产品经理才能操作
        if (bizDomainGroupMapper.countBizGroup(bizDomainGroupId, LocalSessionUtils.getUserInfo().getId()) <= 0
                && bizDomainGroupMapper.countProductLine(bizDomainGroupId, LocalSessionUtils.getUserInfo().getId()) <= 0) {
            throw new BaseBizRuntimeException("只有业务域集里的产品经理才能操作");
        }
    }
}
