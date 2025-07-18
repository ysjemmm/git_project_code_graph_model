package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.ProductDemandGroupItemMapper;
import com.timevale.forward.dal.dao.ProductDemandGroupMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProductDemandGroupService;
import com.timevale.forward.facade.api.query.ProductDemandGroupQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.ProductDemandGroupCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.forward.service.utils.position.PositionUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 产品需求分组服务实现类
 *
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
@Slf4j
@RestService
public class ProductDemandGroupServiceImpl implements ProductDemandGroupService {

    @Resource
    private ProductDemandGroupComponent productDemandGroupComponent;

    @Resource
    private ProductDemandGroupItemComponent productDemandGroupItemComponent;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private LabelComponent labelComponent;

    @Resource
    private BizLabelMapper bizLabelMapper;

    @Resource
    private BizLabelComponent bizLabelComponent;

    @Resource
    private ProductDemandGroupMapper productDemandGroupMapper;

    @Resource
    private ProductDemandGroupItemMapper productDemandGroupItemMapper;

    @Resource
    private ProductLineComponent productLineComponent;

    @Override
    public BaseResult<QueryResultVO<ProductDemandVO>> listProductDemandBacklog(ProductDemandGroupQueryList productDemandGroupQueryList) {
        // mock返回一个QueryResultVO
        QueryResultVO<ProductDemandVO> result = new QueryResultVO<>();
        // mock分析列表为空
        result.setAnalyseVOList(Collections.emptyList());
        // 构造20条mock ProductDemandVO数据
        List<ProductDemandVO> voList = new java.util.ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            ProductDemandVO vo = new ProductDemandVO();
            vo.setId((long) i); // mock主键id
            vo.setName("mock产品需求" + i); // mock名称
            vo.setStatus(1); // mock状态
            vo.setOwner("owner" + i); // mock负责人
            vo.setOwnerId("ownerId" + i); // mock负责人id
            voList.add(vo);
        }
        // 构造分页对象
        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        // 通过反射或直接赋值给items/total等字段（假设有setItems/setTotal方法）
        try {
            java.lang.reflect.Method setItems = pageQueryResult.getClass().getMethod("setItems", List.class);
            setItems.invoke(pageQueryResult, voList); // 设置数据列表
            java.lang.reflect.Method setTotal = pageQueryResult.getClass().getMethod("setTotal", long.class);
            setTotal.invoke(pageQueryResult, 20L); // 设置总数
        } catch (Exception e) {
            // 反射失败忽略
        }
        result.setPageQueryResult(pageQueryResult); // 设置分页结果
        return BaseResult.success(result); // 返回成功结果
    }

    /**
     * 查询业务域需求分组列表
     *
     * @param productDemandGroupQueryList 查询条件
     * @return 产品需求分组VO列表
     */
    @Override
    public BaseResult<PageQueryResult<ProductDemandGroupVO>> listProductDemandGroup(ProductDemandGroupQueryList productDemandGroupQueryList) {
        log.info("产品需求分组接收参数:{}", productDemandGroupQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandGroupListCondition condition = ProductDemandGroupCopier.INSTANCE.convert(productDemandGroupQueryList);
        if (AscriptionEnum.CURRENT_USER.name().equals(productDemandGroupQueryList.getAscription())) {
            condition.getOwnerIds().add(userInfo.getId());
        } else if (AscriptionEnum.DEPARTMENT.name().equals(productDemandGroupQueryList.getAscription())) {
            List<BaseInfoResponse> baseInfos = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));
            String groupId = baseInfos.get(0).getDefaultGroup().getGroupId();
            List<String> accountIds = innerUserPersonClient.getAllByGroupId(groupId);
            log.info("用户默认部门id:{},同部门人员:{}", groupId, accountIds);
            if (!CollectionUtils.isEmpty(productDemandGroupQueryList.getOwnerIds())) {
                accountIds.retainAll(productDemandGroupQueryList.getOwnerIds());
                log.info("用户默认部门id:{},过滤后:{}", groupId, accountIds);
            }
            if (CollectionUtils.isEmpty(accountIds)) {
                //所选人员不在我的部门中
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setOwnerIds(accountIds);
        }
        //是否打标
        if (CollectionUtils.isNotEmpty(productDemandGroupQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(productDemandGroupQueryList.getLabelCategoryIds())) {
            Boolean containLabel = productDemandGroupQueryList.getContainLabel();

            List<Long> newLabelIds = labelComponent.getLabelIds(productDemandGroupQueryList.getLabelIds(), productDemandGroupQueryList.getLabelCategoryIds());

            // 查询包含且类别下没有标签
            if (CollectionUtils.isEmpty(newLabelIds) && containLabel) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }

            // 查询使用这些标签的需求id
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (containLabel) {
                if (CollectionUtils.isEmpty(bizIds)) {
                    return BaseResult.success(ResultUtil.pageEmpty());
                }
                condition.setInProductDemandIds(bizIds);
            } else {
                condition.setNotInProductDemandIds(bizIds);
            }
        }
        // 设置分页
        PageHelper.startPage(productDemandGroupQueryList.getPageNum(), productDemandGroupQueryList.getPageSize());
        List<ProductDemandGroupDO> productDemandGroupListDO = productDemandGroupComponent.list(condition);
        List<ProductDemandGroupVO> productDemandGroupListVO = ProductDemandGroupCopier.INSTANCE.convert(productDemandGroupListDO);
        if (CollectionUtils.isEmpty(productDemandGroupListVO)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        // 遍历分组，获取分组下的需求
        for (ProductDemandGroupVO productDemandGroupVO : productDemandGroupListVO) {
            condition.setGroupId(productDemandGroupVO.getId());
            // 查询分组下的需求
            // TODO 是否需要加分页？
            List<ProductDemandGroupItemListDO> productDemandGroupItemListDOS = productDemandGroupItemComponent.listProductDemand(condition);
            List<ProductDemandGroupItemVO> productDemandGroupItemVOList = convert(productDemandGroupItemListDOS);
            // 获取分组下所有需求的标签
            List<Long> productDemandIds = productDemandGroupItemListDOS.stream().map(ProductDemandGroupItemListDO::getProductDemandId).collect(Collectors.toList());
            Map<Long, List<BizLabelSimpleVO>> bizLabelMap = bizLabelComponent.getBizLabelMap(productDemandIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            // 设置状态名称，优先级名称，需求类型名称，需求标签名称
            for (ProductDemandGroupItemVO a : productDemandGroupItemVOList) {
                a.getProductDemand().setStatusName(ProductDemandStatusEnum.getTextByCode(a.getProductDemand().getStatus()));
                a.getProductDemand().setPriorityName(PriorityEnum.getTextByCode(a.getProductDemand().getPriority()));

                List<BizLabelSimpleVO> labelSimpleVOList = bizLabelMap.get(a.getProductDemand().getId());
                if (CollectionUtils.isNotEmpty(labelSimpleVOList)) {
                    a.getProductDemand().setLabelNames(labelSimpleVOList);
                }

                String typeName = a.getProductDemand().getType().stream()
                        .map(ProductDemandTypeEnum::getTextByCode)
                        .collect(Collectors.joining(","));
                a.getProductDemand().setTypeName(typeName);
            }
            productDemandGroupVO.setProductDemandGroupItems(productDemandGroupItemVOList);
        }
        // 返回分页数据
        PageInfo<ProductDemandGroupDO> pageInfo = new PageInfo<>(productDemandGroupListDO);
        PageQueryResult<ProductDemandGroupVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandGroupListVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    private List<ProductDemandGroupItemVO> convert(List<ProductDemandGroupItemListDO> productDemandGroupItemListDOS) {
        List<ProductDemandGroupItemVO> productDemandGroupItemVOList = new ArrayList<>();
        for (ProductDemandGroupItemListDO productDemandGroupItemListDO : productDemandGroupItemListDOS) {
            ProductDemandGroupItemVO productDemandGroupItemVO = new ProductDemandGroupItemVO();
            productDemandGroupItemVO.setId(productDemandGroupItemListDO.getId());
            productDemandGroupItemVO.setProductDemandGroupId(productDemandGroupItemListDO.getProductDemandGroupId());
            productDemandGroupItemVO.setProductDemandId(productDemandGroupItemListDO.getProductDemandId());
            productDemandGroupItemVO.setPosition(productDemandGroupItemListDO.getPosition());
            productDemandGroupItemVO.setVersion(productDemandGroupItemListDO.getVersion());
            productDemandGroupItemVO.setProductDemand(ProductDemandCopier.INSTANCE.convert(productDemandGroupItemListDO));
            productDemandGroupItemVOList.add(productDemandGroupItemVO);
        }
        return productDemandGroupItemVOList;
    }

    /**
     * 新增产品需求分组
     *
     * @param productDemandGroupAddReq 新增请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProductDemandGroupAddReq productDemandGroupAddReq) {
        log.info("产品需求分组新增接收参数:{}", productDemandGroupAddReq);
        if (productDemandGroupAddReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("产品需求分组名称中请勿包含空格");
        }
        // 只有业务域有里的产品经理才能新增
        List<String> productLineOwners = productLineComponent.getProductLineOwnersInBizDomain(productDemandGroupAddReq.getBizDomainId());
        if (!productLineOwners.contains(LocalSessionUtils.getUserInfo().getId())) {
            throw new BaseBizRuntimeException("只有业务域有里的产品经理才能新增产品需求分组");
        }

        ProductDemandGroupDO productDemandGroupDO = ProductDemandGroupCopier.INSTANCE.toDO(productDemandGroupAddReq);
        ProductDemandGroupDO productDemandGroup = productDemandGroupMapper.getByBizDomainIdAndName(productDemandGroupDO.getBizDomainId(), productDemandGroupDO.getName());
        if (productDemandGroup != null) {
            throw new BaseBizRuntimeException("该产品需求分组名称已存在,请修改后重试");
        }
        double position = 0;
        try {
            position = PositionUtil.generate(productDemandGroupAddReq.getBizDomainId().toString(), System.currentTimeMillis());
        } catch (Exception e) {
            throw new BaseBizRuntimeException("获取产品需求分组位置错误,请修改后重试");
        }
        productDemandGroupDO.setPosition(position);
        productDemandGroupDO.setVersion(0L);
        productDemandGroupMapper.insert(productDemandGroupDO);
        return BaseResult.success(true);
    }

    /**
     * 修改产品需求分组
     *
     * @param productDemandGroupModifyReq 产品需求分组修改请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProductDemandGroupModifyReq productDemandGroupModifyReq) {
        log.info("产品需求分组修改接收参数:{}", productDemandGroupModifyReq);
        if (productDemandGroupModifyReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("产品需求分组名称中请勿包含空格");
        }

        // 只有业务域有里的产品经理才能修改
        List<String> productLineOwners = productLineComponent.getProductLineOwnersInBizDomain(productDemandGroupModifyReq.getBizDomainId());
        if (!productLineOwners.contains(LocalSessionUtils.getUserInfo().getId())) {
            throw new BaseBizRuntimeException("只有业务域有里的产品经理才能修改产品需求分组");
        }

        ProductDemandGroupDO productDemandGroupDO = ProductDemandGroupCopier.INSTANCE.toDO(productDemandGroupModifyReq);
        ProductDemandGroupDO productDemandGroup = productDemandGroupMapper.get(productDemandGroupDO.getId());
        if (productDemandGroup == null) {
            throw new BaseBizRuntimeException("该产品需求分组不存在，请刷新后重试");
        }
        if (!productDemandGroup.getBizDomainId().equals(productDemandGroupDO.getBizDomainId())) {
            throw new BaseBizRuntimeException("该产品需求分组的业务域不能修改,请修改后重试");
        }
        ProductDemandGroupDO oldProductDemandGroup = productDemandGroupMapper.getByBizDomainIdAndName(productDemandGroupDO.getBizDomainId(), productDemandGroupDO.getName());
        if (oldProductDemandGroup != null && !oldProductDemandGroup.getId().equals(productDemandGroupDO.getId())) {
            throw new BaseBizRuntimeException("该产品需求分组名称已存在,请修改后重试");
        }
        productDemandGroupComponent.update(productDemandGroupDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(ProductDemandGroupReq productDemandGroupReq) {
        log.info("产品需求分组删除接收参数:{}", productDemandGroupReq);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String modifyManId = userInfo.getId();
        String modifyMan = userInfo.getFullAlias();
        ProductDemandGroupDO productDemandGroupDO = productDemandGroupMapper.get(productDemandGroupReq.getId());
        // 只有业务域有里的产品经理才能删除
        List<String> productLineOwners = productLineComponent.getProductLineOwnersInBizDomain(productDemandGroupDO.getBizDomainId());
        if (!productLineOwners.contains(modifyManId)) {
            throw new BaseBizRuntimeException("只有业务域有里的产品经理才能删除产品需求分组");
        }

        // 删除产品需求分组
        productDemandGroupMapper.delete(productDemandGroupReq.getId(),modifyManId, modifyMan);
        // 删除产品需求分组下的产品需求
        productDemandGroupItemMapper.deleteByGroupId(productDemandGroupReq.getId(),modifyManId, modifyMan);
        return BaseResult.success(true);
    }

    /**
     * 根据id获取产品需求分组
     *
     * @param id 主键id
     * @return 产品需求分组VO
     */
    @Override
    public BaseResult<ProductDemandGroupVO> getProductDemandGroupById(Long id) {
        ProductDemandGroupDO entity = productDemandGroupComponent.getById(id);
        ProductDemandGroupVO vo = ProductDemandGroupCopier.INSTANCE.convert(entity);
        return BaseResult.success(vo);
    }

    @Override
    public BaseResult<Boolean> bindProject(ProductDemandGroupProjectReq productDemandGroupProjectReq) {
        log.info("产品需求分组绑定接收参数:{}", productDemandGroupProjectReq);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> moveProductDemandGroup(ProductDemandGroupMoveReq productDemandGroupMoveReq) {
        log.info("产品需求移动接收参数:{}", productDemandGroupMoveReq);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> moveProductDemand(ProductDemandGroupMoveReq productDemandGroupMoveReq) {
        log.info("产品需求分组移动接收参数:{}", productDemandGroupMoveReq);
        return BaseResult.success(true);
    }
}
