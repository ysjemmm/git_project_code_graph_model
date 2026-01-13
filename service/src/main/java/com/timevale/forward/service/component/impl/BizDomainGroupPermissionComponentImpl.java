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

    @Override
    public void checkOperationPermissionByProductLineId(Long productLineId) {
        ProductLineDO productLineDO = productLineMapper.get(productLineId);
        if (productLineDO == null || productLineDO.getBizDomainId() == null) {
            throw new BaseBizRuntimeException("产品线不存在");
        }
        // 通过业务域ID查询所属的业务域集
        List<BizDomainGroupRelationDO> relations = bizDomainGroupRelationMapper.selectByBizDomainId(productLineDO.getBizDomainId());
        if (CollectionUtils.isEmpty(relations)) {
            throw new BaseBizRuntimeException("产品线未关联业务域集");
        }
        // 检查用户是否有任一业务域集的权限
        for (BizDomainGroupRelationDO relation : relations) {
            if (bizDomainGroupMapper.countBizGroup(relation.getBizDomainGroupId(), LocalSessionUtils.getUserInfo().getId()) > 0
                    || bizDomainGroupMapper.countProductLine(relation.getBizDomainGroupId(), LocalSessionUtils.getUserInfo().getId()) > 0) {
                return;
            }
        }
        throw new BaseBizRuntimeException("只有业务域集里的产品经理才能操作");
    }
}
