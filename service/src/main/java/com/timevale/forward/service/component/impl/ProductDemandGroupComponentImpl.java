package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.dao.ProductDemandGroupItemMapper;
import com.timevale.forward.dal.dao.ProductDemandGroupMapper;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandGroupDO;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.ProductDemandGroupComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 产品需求分组组件实现类
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
@Component
public class ProductDemandGroupComponentImpl implements ProductDemandGroupComponent {

    @Resource
    private ProductDemandGroupMapper productDemandGroupMapper;

    @Resource
    private ProductDemandGroupItemMapper productDemandGroupItemMapper;

    @Override
    public List<ProductDemandGroupDO> list(ProductDemandGroupListCondition condition) {
        condition.setName(StringUtil.toLikeStr(condition.getName()));
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        condition.setProductDemandQueryExist(condition.judgeProductDemandQueryExist());
        // 分组条件默认值，后面如需求可以在页面传入
        Date now = new Date(); // 当前时间
        Date twoYearsAgo = DateUtil.addYear(now, -2); // 两年前的时间
        List<Integer> statusList = new java.util.ArrayList<>();
        // 遍历ProjectStatusEnum的所有枚举值
        for (ProjectStatusEnum statusEnum : ProjectStatusEnum.values()) {
            if (statusEnum != ProjectStatusEnum.CONCLUSION && 
                statusEnum != ProjectStatusEnum.INVALID && 
                statusEnum != ProjectStatusEnum.CANCELLATION ) {
                statusList.add(statusEnum.getCode()); 
            }
        }
        condition.setGroupStatus(statusList);
        condition.setGroupCreateDateStart(DateUtil.getStartOfDay(twoYearsAgo));
        condition.setGroupCreateDateEnd(DateUtil.getEndOfDay(now));
        return productDemandGroupMapper.list(condition);
    }

    @Override
    public void update(ProductDemandGroupDO productDemandGroupDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        productDemandGroupDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        productDemandGroupDO.setModifyManId(userInfo.getId());
        productDemandGroupMapper.update(productDemandGroupDO);
    }

    @Override
    public List<ProductDemandDO> listProductDemandByGroupId(Long groupId) {
        return Collections.emptyList();
    }

    /**
     * 根据id获取产品需求分组
     * @param id 主键id
     * @return 产品需求分组DO
     */
    @Override
    public ProductDemandGroupDO getById(Long id) {
        return productDemandGroupMapper.get(id);
    }

} 