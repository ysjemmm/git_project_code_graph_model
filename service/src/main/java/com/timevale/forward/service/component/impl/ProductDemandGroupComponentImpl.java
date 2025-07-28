package com.timevale.forward.service.component.impl;

import com.google.common.collect.Lists;
import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.dao.ProductDemandGroupItemMapper;
import com.timevale.forward.dal.dao.ProductDemandGroupMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandGroupDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.facade.api.request.ProjectProductDemandLinkReq;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.ProductDemandGroupComponent;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;

import com.timevale.mandarin.base.util.AssertUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

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
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;
    @Resource
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Override
    public List<ProductDemandGroupDO> list(ProductDemandGroupListCondition condition) {
        condition.setName(StringUtil.toLikeStr(condition.getName()));
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        condition.setProductDemandQueryExist(condition.judgeProductDemandQueryExist());
        // 分组条件默认值，后面如需求可以在页面传入
//        Date now = new Date(); // 当前时间
//        Date twoYearsAgo = DateUtil.addYear(now, -2); // 两年前的时间
//        List<Integer> statusList = new java.util.ArrayList<>();
//        // 遍历ProjectStatusEnum的所有枚举值
//        for (ProjectStatusEnum statusEnum : ProjectStatusEnum.values()) {
//            if (statusEnum != ProjectStatusEnum.CONCLUSION &&
//                statusEnum != ProjectStatusEnum.INVALID &&
//                statusEnum != ProjectStatusEnum.CANCELLATION ) {
//                statusList.add(statusEnum.getCode());
//            }
//        }
//        condition.setGroupStatus(statusList);
//        condition.setGroupCreateDateStart(DateUtil.getStartOfDay(twoYearsAgo));
//        condition.setGroupCreateDateEnd(DateUtil.getEndOfDay(now));
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
    public boolean existProject(Long projectId) {
        return productDemandGroupMapper.countProject(projectId) > 0;
    }

    @Override
    public ProductDemandGroupDO getByProjectId(Long projectId) {
        return productDemandGroupMapper.getByProjectId(projectId);
    }

    @Override
    public void removeProject(Long id) {
        productDemandGroupMapper.removeProject(id);
    }

    @Override
    public void linkOrUnLinkProductDemand(Long projectId, Long productDemandId, LinkOrUnLinkEnum type) {
        if (LinkOrUnLinkEnum.LINK.equals(type)) {
            // 校验关联项目
            ProjectDO projectDO = projectMapper.get(projectId);
            AssertUtil.notNull(projectDO, "项目不存在");
            // 已结项 | 已暂停 | 已中止 | 已废除 不支持关联
            ArrayList<Integer> nonSupportStatuses = Lists.newArrayList(ProjectStatusEnum.INVALID.getCode(),
                    ProjectStatusEnum.SUSPEND.getCode(), ProjectStatusEnum.CONCLUSION.getCode(), ProjectStatusEnum.CANCELLATION.getCode());
            AssertUtil.checkState(!nonSupportStatuses.contains(projectDO.getStatus()),
                    String.format("项目状态为%s,不能关联产品需求", ProjectStatusEnum.getTextByCode(projectDO.getStatus())));
            // 产品需求关联到这个项目
            ProjectProductDemandDO projectProduct = projectProductDemandMapper.getByProductDemandId(productDemandId);
            if (projectProduct == null) {
                // 当前需求没有关联项目，直接关联项目
                ProjectProductDemandLinkReq linkReq = new ProjectProductDemandLinkReq();
                linkReq.setProjectId(projectId);
                linkReq.setType(LinkOrUnLinkEnum.LINK.getCode());
                linkReq.setProductDemandIds(Lists.newArrayList(productDemandId));
                projectProductDemandComponent.linkOrUnLinkProductDemand(linkReq);
            } else if (!Objects.equals(projectProduct.getProjectId(), projectId)) {
                // 当前需求关联的项目不是当前项目, 需要先取消关联再关联
                ProjectProductDemandLinkReq unLinkReq = new ProjectProductDemandLinkReq();
                unLinkReq.setProjectId(projectProduct.getProjectId());
                unLinkReq.setType(LinkOrUnLinkEnum.UN_LINK.getCode());
                unLinkReq.setProductDemandIds(Lists.newArrayList(productDemandId));
                projectProductDemandComponent.linkOrUnLinkProductDemand(unLinkReq);

                ProjectProductDemandLinkReq linkReq = new ProjectProductDemandLinkReq();
                linkReq.setProjectId(projectId);
                linkReq.setType(LinkOrUnLinkEnum.LINK.getCode());
                linkReq.setProductDemandIds(Lists.newArrayList(productDemandId));
                projectProductDemandComponent.linkOrUnLinkProductDemand(linkReq);
            }
        } else {
            ProjectProductDemandDO projectProduct = projectProductDemandMapper.getByProductDemandId(productDemandId);
            // 产品需求关联的项目是当前项目, 需要取消关联
            if (projectProduct != null && Objects.equals(projectProduct.getProjectId(), projectId)) {
                ProjectProductDemandLinkReq unLinkReq = new ProjectProductDemandLinkReq();
                unLinkReq.setProjectId(projectId);
                unLinkReq.setType(LinkOrUnLinkEnum.UN_LINK.getCode());
                unLinkReq.setProductDemandIds(Lists.newArrayList(productDemandId));
                projectProductDemandComponent.linkOrUnLinkProductDemand(unLinkReq);
            }

        }
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