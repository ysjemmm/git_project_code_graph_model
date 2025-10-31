package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.PersonRemoveCondition;
import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.dao.BizDomainGroupMapper;
import com.timevale.forward.dal.dao.BizDomainGroupRelationMapper;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.ProductDemandGroupItemMapper;
import com.timevale.forward.dal.dao.ProductDemandGroupMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.dto.ProductDemandMoveDTO;
import com.timevale.forward.dal.entity.BizDomainGroupRelationDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandGroupDO;
import com.timevale.forward.dal.entity.ProductDemandGroupItemDO;
import com.timevale.forward.dal.entity.ProductDemandGroupItemListDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProductDemandOwnerDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.facade.api.client.ProductDemandGroupService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.ProductDemandGroupQueryList;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupAddReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupInnerAddReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupItemMoveReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupModifyReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupMoveReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupProjectLinkReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupResourcePlanReq;
import com.timevale.forward.facade.api.request.ProductDemandGroupTransferReq;
import com.timevale.forward.facade.api.request.ProductDemandOwnerAddReq;
import com.timevale.forward.facade.api.request.ProjectProductDemandLinkReq;
import com.timevale.forward.facade.api.request.ResourcePlanProductDemandAddReq;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.ProductDemandGroupItemVO;
import com.timevale.forward.facade.api.result.ProductDemandGroupResourcePlanVO;
import com.timevale.forward.facade.api.result.ProductDemandGroupVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ResourcePlanProductDemandOwnerVO;
import com.timevale.forward.facade.api.result.ResourcePlanProductDemandTimeVO;
import com.timevale.forward.facade.api.result.ResourcePlanProductDemandVO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandGroupMoveModeEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProductDemandTypeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.component.LabelComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProductDemandGroupComponent;
import com.timevale.forward.service.component.ProductDemandGroupItemComponent;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.ProductDemandGroupCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.ProductDemandToCopiedMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.duplicate.GroupDuplicateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.forward.service.utils.position.PositionUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
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
    private ProductDemandService productDemandService;

    @Resource
    private GroupDuplicateUtil groupDuplicateUtil;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Resource
    private BizDomainGroupMapper bizDomainGroupMapper;

    @Resource
    private BizDomainGroupRelationMapper bizDomainGroupRelationMapper;

    @Resource
    private PersonComponent personComponent;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    /**
     * 查询业务域内的待规划的产品需求
     *
     * @param productDemandGroupQueryList 产品分组查询条件信息
     * @return 待规划产品需求
     */
    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> listProductDemandBacklog(ProductDemandGroupQueryList productDemandGroupQueryList) {
        log.info("待规划产品需求接收参数:{}", productDemandGroupQueryList);
        ProductDemandGroupListCondition condition = ProductDemandGroupCopier.INSTANCE.convert(productDemandGroupQueryList);
        //是否打标
        if (labelCondition(productDemandGroupQueryList, condition)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        // 分页查询
        PageHelper.startPage(productDemandGroupQueryList.getPageNum(), productDemandGroupQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = productDemandGroupItemComponent.listProductDemandBacklog(condition);

        PageQueryResult<ProductDemandVO> pageQueryResult = groupDuplicateUtil.getDemandVOQueryResultVO(productDemandListDO);
        return BaseResult.success(pageQueryResult);
    }

    private boolean labelCondition(ProductDemandGroupQueryList productDemandGroupQueryList, ProductDemandGroupListCondition condition) {
        if (CollectionUtils.isNotEmpty(productDemandGroupQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(productDemandGroupQueryList.getLabelCategoryIds())) {
            boolean containLabel = productDemandGroupQueryList.getContainLabel() == null || productDemandGroupQueryList.getContainLabel();

            List<Long> newLabelIds = labelComponent.getLabelIds(productDemandGroupQueryList.getLabelIds(), productDemandGroupQueryList.getLabelCategoryIds());

            // 查询包含且类别下没有标签
            if (CollectionUtils.isEmpty(newLabelIds) && containLabel) {
                return true;
            }

            // 查询使用这些标签的需求id
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (containLabel) {
                if (CollectionUtils.isEmpty(bizIds)) {
                    return true;
                }
                condition.setInProductDemandIds(bizIds);
            } else {
                condition.setNotInProductDemandIds(bizIds);
            }
        }
        return false;
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
        if (condition.getGroupOwnerIds() == null) {
            condition.setGroupOwnerIds(Lists.newArrayList());
        }
        if (AscriptionEnum.CURRENT_USER.name().equals(productDemandGroupQueryList.getAscription())) {
            condition.getGroupOwnerIds().add(userInfo.getId());
        } else if (AscriptionEnum.DEPARTMENT.name().equals(productDemandGroupQueryList.getAscription())) {
            List<BaseInfoResponse> baseInfos = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));
            String groupId = baseInfos.get(0).getDefaultGroup().getGroupId();
            List<String> accountIds = innerUserPersonClient.getAllByGroupId(groupId);
            log.info("用户默认部门id:{},同部门人员:{}", groupId, accountIds);
            if (CollectionUtils.isEmpty(accountIds)) {
                //所选人员不在我的部门中
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setGroupOwnerIds(accountIds);
        }
        //是否打标
        if (labelCondition(productDemandGroupQueryList, condition)) {
            return BaseResult.success(ResultUtil.pageEmpty());
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

            // 初始化
            BigDecimal totalUedTime = BigDecimal.ZERO;
            BigDecimal totalBackTime = BigDecimal.ZERO;
            BigDecimal totalFrontTime = BigDecimal.ZERO;
            BigDecimal totalQaTime = BigDecimal.ZERO;
            BigDecimal totalTotalTime = BigDecimal.ZERO;
            BigDecimal totalTransferTime = BigDecimal.ZERO;
            BigDecimal totalProductTime = BigDecimal.ZERO;
            BigDecimal totalOpsTime = BigDecimal.ZERO;
            BigDecimal totalSecurityTime = BigDecimal.ZERO;

            for (ProductDemandGroupItemVO item : productDemandGroupItemVOList) {
                totalUedTime = totalUedTime.add(item.getUedTime() != null ? item.getUedTime() : BigDecimal.ZERO);
                totalBackTime = totalBackTime.add(item.getBackTime() != null ? item.getBackTime() : BigDecimal.ZERO);
                totalFrontTime = totalFrontTime.add(item.getFrontTime() != null ? item.getFrontTime() : BigDecimal.ZERO);
                totalQaTime = totalQaTime.add(item.getQaTime() != null ? item.getQaTime() : BigDecimal.ZERO);
                totalTotalTime = totalTotalTime.add(item.getTotalTime() != null ? item.getTotalTime() : BigDecimal.ZERO);
                totalTransferTime = totalTransferTime.add(item.getTransferTime() != null ? item.getTransferTime() : BigDecimal.ZERO);
                totalProductTime = totalProductTime.add(item.getProductTime() != null ? item.getProductTime() : BigDecimal.ZERO);
                totalOpsTime = totalOpsTime.add(item.getOpsTime() != null ? item.getOpsTime() : BigDecimal.ZERO);
                totalSecurityTime = totalSecurityTime.add(item.getSecurityTime() != null ? item.getSecurityTime() : BigDecimal.ZERO);
            }

            productDemandGroupVO.setTotalUedTime(totalUedTime);
            productDemandGroupVO.setTotalBackTime(totalBackTime);
            productDemandGroupVO.setTotalFrontTime(totalFrontTime);
            productDemandGroupVO.setTotalQaTime(totalQaTime);
            productDemandGroupVO.setTotalTransferTime(totalTransferTime);
            productDemandGroupVO.setTotalProductTime(totalProductTime);
            productDemandGroupVO.setTotalOpsTime(totalOpsTime);
            productDemandGroupVO.setTotalSecurityTime(totalSecurityTime);
            productDemandGroupVO.setTotalTotalTime(totalTotalTime);
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
            ProductDemandVO convert = ProductDemandCopier.INSTANCE.convert(productDemandGroupItemListDO);
            convert.setStatusName(ProductDemandStatusEnum.getTextByCode(convert.getStatus()));
            productDemandGroupItemVO.setProductDemand(convert);
            productDemandGroupItemVO.setUedTime(productDemandGroupItemListDO.getUedTime());
            productDemandGroupItemVO.setBackTime(productDemandGroupItemListDO.getBackTime());
            productDemandGroupItemVO.setFrontTime(productDemandGroupItemListDO.getFrontTime());
            productDemandGroupItemVO.setQaTime(productDemandGroupItemListDO.getQaTime());
            productDemandGroupItemVO.setTransferTime(productDemandGroupItemListDO.getTransferTime());
            productDemandGroupItemVO.setOpsTime(productDemandGroupItemListDO.getOpsTime());
            productDemandGroupItemVO.setSecurityTime(productDemandGroupItemListDO.getSecurityTime());
            productDemandGroupItemVO.setTotalTime(productDemandGroupItemListDO.getTotalTime());
            productDemandGroupItemVOList.add(productDemandGroupItemVO);
        }
        return productDemandGroupItemVOList;
    }

    private void checkOperationPermission(Long bizDomainGroupId) {
        // 只有业务域集有里的产品经理才能新增
        if (bizDomainGroupMapper.countBizGroup(bizDomainGroupId, LocalSessionUtils.getUserInfo().getId()) <= 0
                && bizDomainGroupMapper.countProductLine(bizDomainGroupId, LocalSessionUtils.getUserInfo().getId()) <= 0) {
            throw new BaseBizRuntimeException("只有业务域集里的产品经理才能操作");
        }
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
        checkOperationPermission(productDemandGroupAddReq.getBizDomainGroupId());
        ProductDemandGroupDO productDemandGroupDO = ProductDemandGroupCopier.INSTANCE.toDO(productDemandGroupAddReq);
        ProductDemandGroupDO productDemandGroup = productDemandGroupMapper.getByBizDomainGroupIdAndName(productDemandGroupDO.getBizDomainGroupId(), productDemandGroupDO.getName());
        if (productDemandGroup != null) {
            throw new BaseBizRuntimeException("该产品需求分组名称已存在,请修改后重试");
        }
        BigDecimal position = PositionUtil.generate(productDemandGroupAddReq.getBizDomainGroupId().toString(), System.currentTimeMillis());
        productDemandGroupDO.setPosition(position);
        productDemandGroupDO.setVersion(0L);
        productDemandGroupDO.setIsActive(true);
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
        checkOperationPermission(productDemandGroupModifyReq.getBizDomainGroupId());

        ProductDemandGroupDO productDemandGroupDO = ProductDemandGroupCopier.INSTANCE.toDO(productDemandGroupModifyReq);
        ProductDemandGroupDO productDemandGroup = productDemandGroupMapper.get(productDemandGroupDO.getId());
        if (productDemandGroup == null) {
            throw new BaseBizRuntimeException("该产品需求分组不存在，请刷新后重试");
        }
        if (!Objects.equals(productDemandGroup.getBizDomainGroupId(), productDemandGroupDO.getBizDomainGroupId())) {
            throw new BaseBizRuntimeException("该产品需求分组的业务域集不能修改,请修改后重试");
        }
        ProductDemandGroupDO oldProductDemandGroup = productDemandGroupMapper.getByBizDomainGroupIdAndName(productDemandGroupDO.getBizDomainGroupId(), productDemandGroupDO.getName());
        if (oldProductDemandGroup != null && !Objects.equals(oldProductDemandGroup.getId(), productDemandGroupDO.getId())) {
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
        checkOperationPermission(productDemandGroupDO.getBizDomainGroupId());
        // 删除产品需求分组
        productDemandGroupMapper.delete(productDemandGroupReq.getId(), modifyManId, modifyMan);
        // 删除产品需求分组下的产品需求
        productDemandGroupItemMapper.deleteByGroupId(productDemandGroupReq.getId(), modifyManId, modifyMan);
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
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> moveProductDemandGroup(ProductDemandGroupMoveReq productDemandGroupMoveReq) {
        log.info("产品需求拖动分组接收参数:{}", productDemandGroupMoveReq);
        checkOperationPermission(productDemandGroupMoveReq.getBizDomainGroupId());
        ProductDemandGroupDO targetGroupDO = productDemandGroupMapper.getByIdAndBizDomainGroupId(productDemandGroupMoveReq.getBizDomainGroupId(), productDemandGroupMoveReq.getId());
        if (targetGroupDO == null) {
            throw new BaseBizRuntimeException("产品需求分组不存在, 请刷新后重试");
        }
        ProductDemandGroupDO prevGroupDO = null;
        if (productDemandGroupMoveReq.getPrevId() != null) {
            prevGroupDO = productDemandGroupMapper.getByIdAndBizDomainGroupId(productDemandGroupMoveReq.getBizDomainGroupId(), productDemandGroupMoveReq.getPrevId());
            if (prevGroupDO == null) {
                throw new BaseBizRuntimeException("产品需求分组不存在, 请刷新后重试");
            }
        }
        ProductDemandGroupDO nextGroupDO = null;
        if (productDemandGroupMoveReq.getNextId() != null) {
            nextGroupDO = productDemandGroupMapper.getByIdAndBizDomainGroupId(productDemandGroupMoveReq.getBizDomainGroupId(), productDemandGroupMoveReq.getNextId());
            if (nextGroupDO == null) {
                throw new BaseBizRuntimeException("产品需求分组不存在, 请刷新后重试");
            }
        }
        ProductDemandGroupDO targetNextGroupDO = productDemandGroupMapper.getNextByPosition(productDemandGroupMoveReq.getBizDomainGroupId(), targetGroupDO.getPosition());
        ProductDemandGroupDO targetPreGroupDO = productDemandGroupMapper.getPreByPosition(productDemandGroupMoveReq.getBizDomainGroupId(), targetGroupDO.getPosition());
        boolean preConditionEqual = (prevGroupDO == null && targetPreGroupDO == null) || (prevGroupDO != null && targetPreGroupDO != null && prevGroupDO.getId().equals(targetPreGroupDO.getId()));
        boolean nextConditionEqual = (nextGroupDO == null && targetNextGroupDO == null) || (nextGroupDO != null && targetNextGroupDO != null && nextGroupDO.getId().equals(targetNextGroupDO.getId()));
        if (preConditionEqual && nextConditionEqual) {
            log.info("产品需求分组位置未变化，无需移动");
            return BaseResult.success(true);
//            throw new BaseBizRuntimeException("产品需求分组位置未变化，无需移动");
        }
        // 定义 getPrevByPosition 和 getNextByPosition 函数
        BiFunction<Long, BigDecimal, BigDecimal> getPrevByPosition = (bizDomainGroupId, position) -> {
            ProductDemandGroupDO preByPosition = productDemandGroupMapper.getPreByPosition(bizDomainGroupId, position);
            return preByPosition == null ? null : preByPosition.getPosition();
        };
        BiFunction<Long, BigDecimal, BigDecimal> getNextByPosition = (bizDomainGroupId, position) -> {
            ProductDemandGroupDO nextByPosition = productDemandGroupMapper.getNextByPosition(bizDomainGroupId, position);
            return nextByPosition == null ? null : nextByPosition.getPosition();
        };
        BiFunction<Long, Long, BigDecimal> getPosition = (bizDomainGroupId, id) -> {
            ProductDemandGroupDO nextByPosition = productDemandGroupMapper.getByIdAndBizDomainGroupId(bizDomainGroupId, id);
            return nextByPosition == null ? null : nextByPosition.getPosition();
        };
        Pair<BigDecimal, Boolean> position = productDemandGroupItemComponent.calculateNewPosition(productDemandGroupMoveReq.getPrevId(),
                productDemandGroupMoveReq.getNextId(),
                productDemandGroupMoveReq.getBizDomainGroupId(),
                productDemandGroupMoveReq.getBizDomainGroupId(),
                getPosition,
                getPrevByPosition,
                getNextByPosition
        );
        // position和前后相同，说明需要重新排序
        if (!position.getSecond()) {
            productDemandGroupComponent.resetPosition(productDemandGroupMoveReq.getBizDomainGroupId());
            position = productDemandGroupItemComponent.calculateNewPosition(productDemandGroupMoveReq.getPrevId(),
                    productDemandGroupMoveReq.getNextId(),
                    productDemandGroupMoveReq.getBizDomainGroupId(),
                    productDemandGroupMoveReq.getBizDomainGroupId(),
                    getPosition,
                    getPrevByPosition,
                    getNextByPosition
            );
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String modifyManId = userInfo.getId();
        String modifyMan = userInfo.getFullAlias();
        val updateGroupDO = new ProductDemandGroupDO().setPosition(position.getFirst())
                .setVersion(targetGroupDO.getVersion());
        updateGroupDO.setId(targetGroupDO.getId());
        updateGroupDO.setModifyMan(modifyMan);
        updateGroupDO.setModifyManId(modifyManId);
        try {
            int updatePosition = productDemandGroupMapper.updatePosition(updateGroupDO);
            if (updatePosition != 1) {
                log.error("更新产品需求分组位置失败：{}", updateGroupDO);
                throw new BaseBizRuntimeException("操作失败，请刷新页面重试");
            }
        } catch (DuplicateKeyException e) {
            log.error("更新产品需求分组位置失败：{}", updateGroupDO, e);
            throw new BaseBizRuntimeException("操作失败，请刷新页面重试");
        }

        // TODO 如果更新失败【乐观锁更新失败，唯一键冲突失败】， 重试或者提示刷新页面重试
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> moveProductDemand(ProductDemandGroupItemMoveReq productDemandGroupItemMoveReq) {
        log.info("产品需求拖动接收参数:{}", productDemandGroupItemMoveReq);
        // 校验操作权限
        checkOperationPermission(productDemandGroupItemMoveReq.getBizDomainGroupId());
        // 拖动产品需求到分组
        ProductDemandMoveDTO productDemandMoveDTO = productDemandGroupItemComponent.moveProductDemand(productDemandGroupItemMoveReq);
        if (productDemandMoveDTO.isIgnore()) {
            return BaseResult.success(true);
        }
        // 需求和项目关联
        if (Objects.equals(ProductDemandGroupMoveModeEnum.MOVE_IN.getCode(), productDemandGroupItemMoveReq.getMode())) {
            // 如果分组已经绑定了项目，需要将产品需求关联项目
            final ProductDemandGroupDO productDemandGroupDO = productDemandGroupComponent.getById(productDemandMoveDTO.getTargetGroupId());
            if (productDemandGroupDO != null && productDemandGroupDO.getProjectId() != null) {
                // moveIn是productDemandGroupItemMoveReq.getId()为产品需求id
                productDemandGroupComponent.linkOrUnLinkProductDemand(productDemandGroupDO.getProjectId(), productDemandMoveDTO.getProductDemandId(), LinkOrUnLinkEnum.LINK);
            }
        } else if (Objects.equals(ProductDemandGroupMoveModeEnum.MOVE_OUT.getCode(), productDemandGroupItemMoveReq.getMode())) {
            // 如果分组已经绑定了项目， 需要将产品需求取消关联
            final ProductDemandGroupDO productDemandGroupDO = productDemandGroupComponent.getById(productDemandMoveDTO.getMoveGroupId());
            if (productDemandGroupDO != null && productDemandGroupDO.getProjectId() != null) {
                productDemandGroupComponent.linkOrUnLinkProductDemand(productDemandGroupDO.getProjectId(), productDemandMoveDTO.getProductDemandId(), LinkOrUnLinkEnum.UN_LINK);
            }
        } else {
            // 如果拖出分组已经绑定了项目， 需要将产品需求取消关联原项目
            final ProductDemandGroupDO moveProductDemandGroupDO = productDemandGroupComponent.getById(productDemandMoveDTO.getMoveGroupId());
            if (moveProductDemandGroupDO != null && moveProductDemandGroupDO.getProjectId() != null) {
                productDemandGroupComponent.linkOrUnLinkProductDemand(moveProductDemandGroupDO.getProjectId(), productDemandMoveDTO.getProductDemandId(), LinkOrUnLinkEnum.UN_LINK);
            }

            // 如果分组已经绑定了项目，需要将产品需求绑定项目
            final ProductDemandGroupDO targetProductDemandGroupDO = productDemandGroupComponent.getById(productDemandMoveDTO.getTargetGroupId());
            if (targetProductDemandGroupDO != null && targetProductDemandGroupDO.getProjectId() != null) {
                productDemandGroupComponent.linkOrUnLinkProductDemand(targetProductDemandGroupDO.getProjectId(), productDemandMoveDTO.getProductDemandId(), LinkOrUnLinkEnum.LINK);
            }
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnlinkProject(ProductDemandGroupProjectLinkReq productDemandGroupItemMoveReq) {
        log.info("关联or取消关联项目需求,参数:{}", productDemandGroupItemMoveReq);
        Long projectId = productDemandGroupItemMoveReq.getProjectId();
        Long groupId = productDemandGroupItemMoveReq.getProductDemandGroupId();
        ProductDemandGroupDO productDemandGroupDO = productDemandGroupComponent.getById(groupId);
        AssertUtil.notNull(productDemandGroupDO, "分组不存在");
        if (LinkOrUnLinkEnum.LINK.getCode().equals(productDemandGroupItemMoveReq.getType())) {
            // 校验关联项目
            ProjectDO projectDO = projectMapper.get(projectId);
            AssertUtil.notNull(projectDO, "项目不存在");
            // 客开项目 || 内部项目 || PBG项目/1-N客开项目 不能关联分组
            if (projectDO.getCustomerDev() != 0 || projectDO.getCategory() != 0 || projectDO.getKind() == 2) {
                throw new BaseBizRuntimeException("项目类型错误，不能关联分组");
            }
            // 已结项 | 已暂停 | 已中止 | 已废除 不支持关联
            ArrayList<Integer> nonSupportStatuses = Lists.newArrayList(ProjectStatusEnum.INVALID.getCode(),
                    ProjectStatusEnum.SUSPEND.getCode(), ProjectStatusEnum.CONCLUSION.getCode(), ProjectStatusEnum.CANCELLATION.getCode());
            AssertUtil.checkState(!nonSupportStatuses.contains(projectDO.getStatus()),
                    String.format("项目状态为%s,不能关联分组", ProjectStatusEnum.getTextByCode(projectDO.getStatus())));
//            boolean existProject = productDemandGroupComponent.existProject(projectId);
//            AssertUtil.checkState(!existProject, "当前项目已经被关联，不能重复关联");

            // 更新分组的项目
            val newProductDemandGroupDO = new ProductDemandGroupDO().setProjectId(projectId);
            newProductDemandGroupDO.setId(groupId);
            productDemandGroupComponent.update(newProductDemandGroupDO);
            // 分组内的产品需求更项目关联
            List<ProductDemandGroupItemDO> groupItems = productDemandGroupItemMapper.getByGroupId(groupId);
            HashMap<Long, List<Long>> unlinkProductDemandIdMap = Maps.newHashMap();
            List<Long> linkProductDemandIds = Lists.newArrayList();
            for (ProductDemandGroupItemDO groupItem : groupItems) {
                // 产品需求关联到这个项目
                ProjectProductDemandDO projectProduct = projectProductDemandMapper.getByProductDemandId(groupItem.getProductDemandId());
                if (projectProduct == null) {
                    // 当前需求没有关联项目，直接关联项目
                    linkProductDemandIds.add(groupItem.getProductDemandId());
                } else if (!Objects.equals(projectProduct.getProjectId(), projectId)) {
                    // 当前需求关联的项目不是当前项目, 需要先取消关联再关联
                    unlinkProductDemandIdMap.computeIfAbsent(projectProduct.getProjectId(), k -> new ArrayList<>()).add(groupItem.getProductDemandId());
                    linkProductDemandIds.add(groupItem.getProductDemandId());
                }
            }
            // 需求取消关联项目
            if (!unlinkProductDemandIdMap.isEmpty()) {
                unlinkProductDemandIdMap.forEach((unlinkProjectId, unlinkProductDemandIds) -> {
                    ProjectProductDemandLinkReq unLinkReq = new ProjectProductDemandLinkReq();
                    unLinkReq.setProjectId(unlinkProjectId);
                    unLinkReq.setType(LinkOrUnLinkEnum.UN_LINK.getCode());
                    unLinkReq.setProductDemandIds(unlinkProductDemandIds);
                    projectProductDemandComponent.linkOrUnLinkProductDemand(unLinkReq);
                });
            }

            // 需求关联项目
            if (!linkProductDemandIds.isEmpty()) {
                ProjectProductDemandLinkReq linkReq = new ProjectProductDemandLinkReq();
                linkReq.setProjectId(projectId);
                linkReq.setType(LinkOrUnLinkEnum.LINK.getCode());
                linkReq.setProductDemandIds(linkProductDemandIds);
                projectProductDemandComponent.linkOrUnLinkProductDemand(linkReq);
            }
        } else {
            // 删除分组的项目
            productDemandGroupComponent.removeProject(groupId);
        }
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> transferProductDemandGroup(ProductDemandGroupTransferReq productDemandGroupTransferReq) {
        ProductDemandGroupDO productDemandGroupDO = productDemandGroupMapper.get(productDemandGroupTransferReq.getId());
        AssertUtil.checkState(productDemandGroupDO != null, "规划分组不存在");
        if (Objects.equals(productDemandGroupDO.getBizDomainGroupId(), productDemandGroupTransferReq.getBizDomainGroupId())) {
            return BaseResult.success(true);
        }
        // 校验操作权限
        checkOperationPermission(productDemandGroupDO.getBizDomainGroupId());
        // 校验业务域集范围
        List<BizDomainGroupRelationDO> sourceBizDomainGroupRelationDOS = bizDomainGroupRelationMapper.selectByBizDomainGroupId(productDemandGroupDO.getBizDomainGroupId());
        List<BizDomainGroupRelationDO> targetBizDomainGroupRelationDOS = bizDomainGroupRelationMapper.selectByBizDomainGroupId(productDemandGroupTransferReq.getBizDomainGroupId());

        // 提取target中的所有bizDomainId
        Set<Long> targetBizDomainIds = targetBizDomainGroupRelationDOS.stream()
                .map(BizDomainGroupRelationDO::getBizDomainId)
                .collect(Collectors.toSet());

        // 检查source中的所有bizDomainId是否都在target中
        boolean containsAll = sourceBizDomainGroupRelationDOS.stream()
                .map(BizDomainGroupRelationDO::getBizDomainId)
                .allMatch(targetBizDomainIds::contains);
        AssertUtil.checkState(containsAll, "目标业务域集的范围没有包含当前业务域集，不能操作");
        // 转交
        ProductDemandGroupDO groupDO = new ProductDemandGroupDO();
        groupDO.setId(productDemandGroupTransferReq.getId());
        groupDO.setBizDomainGroupId(productDemandGroupTransferReq.getBizDomainGroupId());
        groupDO.setPosition(PositionUtil.generate(productDemandGroupTransferReq.getBizDomainGroupId().toString(), System.currentTimeMillis()));
        productDemandGroupMapper.update(groupDO);
        return BaseResult.success(true);
    }

    private List<ResourcePlanProductDemandAddReq> distinctResourceTypeAndOwner (ProductDemandGroupResourcePlanReq productDemandGroupResourcePlanReq) {
        List<ResourcePlanProductDemandAddReq> productDemands = productDemandGroupResourcePlanReq.getProductDemands();
        if (CollUtil.isEmpty(productDemands)) {
            return Collections.emptyList();
        }
        boolean existDistinctResourceType = productDemands.stream().anyMatch(d ->
                d.getProductDemandOwners().stream().distinct().count() != d.getProductDemandOwners().size());
        if (existDistinctResourceType) {
            AssertUtil.checkState(false, "产品需求-资源规划中每个需求不能存在重复的资源类型");
        }
        productDemands.forEach(d -> {
            if (CollectionUtils.isEmpty(d.getProductDemandOwners())) {
                return;
            }
            Map<String, List<ProductDemandOwnerAddReq>> resourceType2Owner =
                    d.getProductDemandOwners().stream()
                            .collect(Collectors.groupingBy(
                                    ProductDemandOwnerAddReq::getResourceType,
                                    Collectors.collectingAndThen(
                                            Collectors.toMap(
                                                    ProductDemandOwnerAddReq::getOwnerId,
                                                    Function.identity(),
                                                    (e, r) -> e,
                                                    LinkedHashMap::new
                                            ),
                                            map -> new ArrayList<>(map.values())
                                    )
                            ));
            d.setProductDemandOwners(resourceType2Owner.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
        });
        return productDemands;
    }

    private void batchUpsertProductDemandOwners(Collection<ProductDemandOwnerDO> addOwners, Collection<ProductDemandOwnerDO> updateOwners,@NonNull String operatorId, @NonNull String operator) {
        if (CollUtil.isEmpty(addOwners) && CollUtil.isEmpty(updateOwners)) {
            return;
        }

        List<ProductDemandOwnerDO> upsertOwners = new ArrayList<ProductDemandOwnerDO>(ObjectUtil.defaultIfNull(addOwners, Collections.emptyList())){{
            addAll(ObjectUtil.defaultIfNull(updateOwners, Collections.emptyList()));
        }};
        productDemandMapper.batchUpsertProductDemandOwners(upsertOwners, operatorId, operator);

        if (CollUtil.isNotEmpty(addOwners)) {
            Map<Long, List<ProductDemandOwnerDO>> demandId2Owners = addOwners.stream().collect(Collectors.groupingBy(ProductDemandOwnerDO::getProductDemandId));
            Map<Long, ProductDemandDO> demandId2Demand = productDemandMapper.selectByIdList(demandId2Owners.keySet()).stream().collect(Collectors.toMap(e->e.getId(), Function.identity(), (e,r)->e));
            demandId2Owners.forEach((demandId, owners)->{
                Set<PersonAddReq> recipients = owners.stream().map(o -> new PersonAddReq().setUserName(o.getOwner()).setUserId(o.getOwnerId()))
                        .collect(Collectors.toSet());
                personComponent.add(recipients, demandId, PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
                ProductDemandDO productDemandDO = demandId2Demand.get(demandId);
                messageEventPublisher.publish(new ProductDemandToCopiedMsgEvent(
                        this,
                        demandId,
                        productDemandDO.getCreateMan(),
                        recipients.stream().map(r->r.getUserId()).collect(Collectors.toList()),
                        productDemandDO.getName()
                ));
            });
        }
    }

    private void batchDeleteProductDemandOwners(Collection<ProductDemandOwnerDO> owners, @NonNull String operatorId, @NonNull String operator) {
        if (CollUtil.isEmpty(owners)) {
            return;
        }
        Set<Long> ids = owners.stream().map(ProductDemandOwnerDO::getId).collect(Collectors.toSet());
        productDemandMapper.batchDeleteProductDemandOwners(ids, operatorId, operator);
    }

    private void updateDemandResourceTime (@NonNull ProductDemandGroupResourcePlanReq resourcePlanReq) {
        if (CollUtil.isEmpty(resourcePlanReq.getProductDemands())) {
            return;
        }
        List<ProductDemandDO> toUpdateResourceTime = resourcePlanReq.getProductDemands().stream()
                .map(e -> ProductDemandCopier.INSTANCE.convertFromResourceDetail(e.getProductDemandTime()))
                .filter(Objects::nonNull)
                .peek(e -> e.setTotalTime(
                        ObjectUtil.defaultIfNull(e.getUedTime(), BigDecimal.ZERO)
                                .add(ObjectUtil.defaultIfNull(e.getBackTime(), BigDecimal.ZERO))
                                .add(ObjectUtil.defaultIfNull(e.getFrontTime(), BigDecimal.ZERO))
                                .add(ObjectUtil.defaultIfNull(e.getQaTime(), BigDecimal.ZERO))
                                .add(ObjectUtil.defaultIfNull(e.getOpsTime(), BigDecimal.ZERO))
                                .add(ObjectUtil.defaultIfNull(e.getSecurityTime(), BigDecimal.ZERO))
                                .add(ObjectUtil.defaultIfNull(e.getProductTime(), BigDecimal.ZERO))
                ))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(toUpdateResourceTime)) {
            productDemandMapper.updateResourceTime(toUpdateResourceTime);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> upsertResourcePlan(ProductDemandGroupResourcePlanReq productDemandGroupResourcePlanReq, boolean isAloneUpdate, boolean isUpdateTime) {
        List<ResourcePlanProductDemandAddReq> productDemands = distinctResourceTypeAndOwner(productDemandGroupResourcePlanReq);

        Map<Long, List<ProductDemandOwnerDO>> existedOwnersMap = new HashMap<>();
        if (!isAloneUpdate) {
            existedOwnersMap = productDemandMapper.listProductDemandOwnersByGroupId(productDemandGroupResourcePlanReq.getProductDemandGroupId())
                    .stream().collect(Collectors.groupingBy(ProductDemandOwnerDO::getProductDemandId));
        } else {
            List<Long> productDemandIds = productDemands.stream().map(ResourcePlanProductDemandAddReq::getProductDemandId).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(productDemandIds)) {
                existedOwnersMap = productDemandMapper.listProductDemandOwnersByIds(productDemandIds).stream().collect(Collectors.groupingBy(ProductDemandOwnerDO::getProductDemandId));
            }
        }
        if (CollectionUtils.isEmpty(productDemands)) {
            return BaseResult.success(true);
        }

        final Set<ProductDemandOwnerDO> waitAddOwners = new LinkedHashSet<>();
        final Set<ProductDemandOwnerDO> waitUpdateOwners = new LinkedHashSet<>();
        final Set<ProductDemandOwnerDO> waitDeleteOwners = new LinkedHashSet<>();

        Map<Long, List<ProductDemandOwnerDO>> finalExistedOwnersMap = existedOwnersMap;
        productDemands.forEach(d -> {
            if (CollUtil.isEmpty(d.getProductDemandOwners())) {
                waitDeleteOwners.addAll(finalExistedOwnersMap.getOrDefault(d.getProductDemandId(), Collections.emptyList()));
            }
        });
        List<ProductDemandOwnerAddReq> productDemandOwners = productDemands.stream().flatMap(d -> d.getProductDemandOwners().stream()
                        .peek(o -> o.setProductDemandId(d.getProductDemandId()))
                ).collect(Collectors.toList());
        for (ProductDemandOwnerAddReq productDemandOwner : productDemandOwners) {
            Set<ProductDemandOwnerDO> nowOwners = new LinkedHashSet<>(ProductDemandCopier.INSTANCE.convertList(productDemandOwners));
            Set<ProductDemandOwnerDO> existedOwners = new LinkedHashSet<>(existedOwnersMap.getOrDefault(productDemandOwner.getProductDemandId(), Collections.emptyList()));
            if (CollUtil.isEmpty(existedOwners)) {
                waitAddOwners.addAll(nowOwners);
                continue;
            }
            waitAddOwners.addAll(nowOwners.stream().filter(item -> !existedOwners.contains(item)).collect(Collectors.toList()));
            waitUpdateOwners.addAll(nowOwners.stream().filter(existedOwners::contains).collect(Collectors.toList()));
            waitDeleteOwners.addAll(existedOwners.stream().filter(item -> !nowOwners.contains(item)).collect(Collectors.toList()));
        }

        batchUpsertProductDemandOwners(waitAddOwners, waitUpdateOwners, productDemandGroupResourcePlanReq.getOperatorId(), productDemandGroupResourcePlanReq.getOperator());
        batchDeleteProductDemandOwners(waitDeleteOwners, productDemandGroupResourcePlanReq.getOperatorId(), productDemandGroupResourcePlanReq.getOperator());
        if (isUpdateTime) {
            updateDemandResourceTime(productDemandGroupResourcePlanReq);
        }

        // 一个需求可能存在某个人既是前端又是后端这种情况，而前端移除它负责人身份，后端添加它为负责人，这种交叉情况需要特判
        List<ProductDemandOwnerDO> realDeletes = waitDeleteOwners.stream().filter(o -> {
            boolean isAdd = waitAddOwners.stream().anyMatch(e -> e.getOwnerId().equals(o.getOwnerId()) && e.getProductDemandId().equals(o.getProductDemandId()));
            boolean isUpdate = waitUpdateOwners.stream().anyMatch(e -> e.getOwnerId().equals(o.getOwnerId()) && e.getProductDemandId().equals(o.getProductDemandId()));
            return !isAdd && !isUpdate;
        }).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(realDeletes)) {
            PersonRemoveCondition removeCondition = PersonRemoveCondition.builder()
                    .type(PersonTypeEnum.PRODUCT_DEMAND_CC.getCode())
                    .mainId2PersonId(realDeletes.stream().map(o -> Pair.of(o.getProductDemandId(), o.getOwnerId())).collect(Collectors.toList()))
                    .build();
            personComponent.remove(removeCondition, productDemandGroupResourcePlanReq.getOperatorId(), productDemandGroupResourcePlanReq.getOperator());
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> addDemand(ProductDemandGroupInnerAddReq productDemandGroupInnerAddReq) {
        ProductDemandAddReq productDemandAddReq = new ProductDemandAddReq();
        BeanUtils.copyProperties(productDemandGroupInnerAddReq, productDemandAddReq);
        productDemandService.add(productDemandAddReq);
        // 根据名称获取需求
        ProductDemandDO productDemandDO = productDemandMapper.getByName(productDemandAddReq.getName());
        AssertUtil.checkState(productDemandDO != null, "需求不存在");

        ProductDemandGroupItemMoveReq productDemandGroupItemMoveReq = new ProductDemandGroupItemMoveReq();
        BeanUtils.copyProperties(productDemandGroupInnerAddReq, productDemandGroupItemMoveReq);
        productDemandGroupItemMoveReq.setMode(ProductDemandGroupMoveModeEnum.MOVE_IN.getCode());
        productDemandGroupItemMoveReq.setId(productDemandDO.getId());
        productDemandGroupItemMoveReq.setPrevId(null);
        moveProductDemand(productDemandGroupItemMoveReq);
        return BaseResult.success(true);
    }

    private BigDecimal getResourceTimeFromDemandDO (@NonNull ResourcePlanProductDemandTimeVO item, @NonNull String resourceType) {
        switch (resourceType) {
            case "frontend": return item.getFrontTime();
            case "backend": return item.getBackTime();
            case "qa": return item.getQaTime();
            case "ued": return item.getUedTime();
            case "product": return item.getProductTime();
            case "ops": return item.getOpsTime();
            case "security": return item.getSecurityTime();
            default: return BigDecimal.ZERO;
        }
    }

    @Override
    public BaseResult<ProductDemandGroupResourcePlanVO> getResourcePlan(@NonNull Long bizDomainGroupId, @NonNull Long productDemandGroupId) {
        ProductDemandGroupResourcePlanVO demandGroupResourcePlanVO = new ProductDemandGroupResourcePlanVO()
                .setProductDemandGroupId(productDemandGroupId)
                .setEditable(true)
                .setProductDemands(new ArrayList<>());

        List<ProductDemandGroupItemDO> groupItems = productDemandGroupItemMapper.getByGroupId(productDemandGroupId);
        if (CollectionUtils.isEmpty(groupItems)) {
            return BaseResult.success(demandGroupResourcePlanVO);
        }

        // 注意需要报错 demand 结果的顺序和 groupItems 的顺序一致
        Set<Long> orderedDemandIds = groupItems.stream().map(ProductDemandGroupItemDO::getProductDemandId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ProductDemandDO> demandMap = productDemandMapper.selectByIdList(orderedDemandIds).stream().collect(Collectors.toMap(ProductDemandDO::getId, Function.identity()));
        Map<Long, ResourcePlanProductDemandTimeVO> id2Demands = new LinkedHashMap<>();
        for (Long id : orderedDemandIds) {
            if (demandMap.containsKey(id)) {
                id2Demands.put(id, ProductDemandCopier.INSTANCE.convertToResourceDetail(demandMap.get(id)));
            }
        }

        final Map<Long,List<ProductDemandOwnerDO>> productDemandId2Owners = productDemandMapper.listProductDemandOwners(id2Demands.keySet()).stream()
                .collect(Collectors.groupingBy(ProductDemandOwnerDO::getProductDemandId));
        Map<Long, List<BizLabelSimpleVO>> productDemandId2Label = bizLabelComponent.getBizLabelMap(new ArrayList<>(id2Demands.keySet()), BizTypeEnum.PRODUCT_DEMAND.getCode());
        Map<Long, String> productDemandId2Name = productDemandMapper.selectByIdList(id2Demands.keySet()).stream()
                .collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getName, (o, n) -> o));

        id2Demands.forEach((id, item) -> {
            ResourcePlanProductDemandVO resourcePlanProductDemandVO = new ResourcePlanProductDemandVO();
            resourcePlanProductDemandVO.setId(id);
            resourcePlanProductDemandVO.setName(productDemandId2Name.get(id));
            resourcePlanProductDemandVO.setLabelNames(productDemandId2Label.getOrDefault(id, Collections.emptyList()));
            resourcePlanProductDemandVO.setProductDemandDetail(item);
            resourcePlanProductDemandVO.setOwners(new HashMap<>());
            demandGroupResourcePlanVO.getProductDemands().add(resourcePlanProductDemandVO);
        });

        if (MapUtils.isEmpty(productDemandId2Owners)) {
            return BaseResult.success(demandGroupResourcePlanVO);
        }

        productDemandId2Owners.forEach((productDemandId, owners) -> {
            Optional<ResourcePlanProductDemandVO> demandOp = demandGroupResourcePlanVO.getProductDemands().stream().filter(d -> d.getId().equals(productDemandId))
                    .findFirst();
            demandOp.ifPresent(resourcePlanProductDemandVO -> resourcePlanProductDemandVO.setOwners(owners.stream()
                    .map(ProductDemandCopier.INSTANCE::convert)
                    .peek(d -> {
                        ResourcePlanProductDemandTimeVO productDemandDO = id2Demands.get(d.getProductDemandId());
                        if (productDemandDO != null) {
                            d.setResourceTime(getResourceTimeFromDemandDO(productDemandDO, d.getResourceType()));
                        }
                    }).collect(Collectors.groupingBy(ResourcePlanProductDemandOwnerVO::getResourceType))));
        });
        return BaseResult.success(demandGroupResourcePlanVO);
    }
}
