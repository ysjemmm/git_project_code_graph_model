package com.timevale.forward.service.component.impl;

// 引入必要的包
import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.dao.ProductDemandGroupItemMapper;
import com.timevale.forward.dal.entity.ProductDemandGroupItemListDO;
import com.timevale.forward.service.component.ProductDemandGroupItemComponent;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 产品-分组关系表 组件实现类
 */
@Component // Spring组件注解
public class ProductDemandGroupItemComponentImpl implements ProductDemandGroupItemComponent {
    // 注入Mapper
    @Resource
    private ProductDemandGroupItemMapper productDemandGroupItemMapper;


    @Override
    public void deleteByGroupId(Long groupId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String modifyManId = userInfo.getId();
        String modifyMan = userInfo.getFullAlias();
        productDemandGroupItemMapper.deleteByGroupId(groupId, modifyManId, modifyMan);
    }

    // 查询某分组下所有产品需求
    @Override
    public List<ProductDemandGroupItemListDO> listProductDemand(ProductDemandGroupListCondition condition) {
        condition.setName(StringUtil.toLikeStr(condition.getName()));
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        condition.setProductDemandQueryExist(condition.judgeProductDemandQueryExist());
        return productDemandGroupItemMapper.listProductDemand(condition);
    }
} 