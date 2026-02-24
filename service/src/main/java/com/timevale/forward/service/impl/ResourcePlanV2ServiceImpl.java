package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.PersonRemoveCondition;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandOwnerTimeMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandGroupItemDO;
import com.timevale.forward.dal.entity.ProductDemandOwnerTimeDO;
import com.timevale.forward.facade.api.client.ResourcePlanV2Service;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ResourcePlanV2SaveReq;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.ResourcePlanV2VO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.PersonLevelEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.observer.event.ProductDemandToCopiedMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.dal.dao.ProductDemandGroupItemMapper;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资源规划V2服务实现
 *
 * @author kiro
 * @date 2026-02-24
 */
@Slf4j
@RestService
public class ResourcePlanV2ServiceImpl implements ResourcePlanV2Service {

    @Resource
    private ProductDemandOwnerTimeMapper ownerTimeMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProductDemandGroupItemMapper productDemandGroupItemMapper;

    @Resource
    private BizLabelComponent bizLabelComponent;

    @Resource
    private PersonComponent personComponent;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Override
    public BaseResult<ResourcePlanV2VO> getResourcePlanV2(Long bizDomainGroupId, Long productDemandGroupId) {
        ResourcePlanV2VO vo = new ResourcePlanV2VO()
                .setProductDemandGroupId(productDemandGroupId)
                .setDemands(new ArrayList<>());

        // 1. 获取分组下的需求（保持排序）
        List<ProductDemandGroupItemDO> groupItems = productDemandGroupItemMapper.getByGroupId(productDemandGroupId);
        if (CollUtil.isEmpty(groupItems)) {
            return BaseResult.success(vo);
        }

        Set<Long> orderedDemandIds = groupItems.stream()
                .map(ProductDemandGroupItemDO::getProductDemandId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 2. 查询需求基本信息
        Map<Long, ProductDemandDO> demandMap = productDemandMapper.selectByIdList(orderedDemandIds).stream()
                .collect(Collectors.toMap(ProductDemandDO::getId, Function.identity()));

        // 3. 查询标签
        Map<Long, List<BizLabelSimpleVO>> labelMap = bizLabelComponent.getBizLabelMap(
                new ArrayList<>(orderedDemandIds), BizTypeEnum.PRODUCT_DEMAND.getCode());

        // 4. 从新表查询资源分配
        List<ProductDemandOwnerTimeDO> ownerTimes = ownerTimeMapper.listByProductDemandIds(orderedDemandIds);
        Map<Long, List<ProductDemandOwnerTimeDO>> ownerTimeMap = ownerTimes.stream()
                .collect(Collectors.groupingBy(ProductDemandOwnerTimeDO::getProductDemandId));

        // 5. 组装返回
        for (Long demandId : orderedDemandIds) {
            ProductDemandDO demand = demandMap.get(demandId);
            if (demand == null) continue;

            ResourcePlanV2VO.DemandWithOwnerTimes demandVO = new ResourcePlanV2VO.DemandWithOwnerTimes();
            demandVO.setId(demandId);
            demandVO.setName(demand.getName());
            demandVO.setLabelNames(labelMap.getOrDefault(demandId, Collections.emptyList()));

            List<ProductDemandOwnerTimeDO> times = ownerTimeMap.getOrDefault(demandId, Collections.emptyList());
            demandVO.setOwnerTimes(times.stream().map(t -> {
                ResourcePlanV2VO.OwnerTimeItem item = new ResourcePlanV2VO.OwnerTimeItem();
                item.setId(t.getId());
                item.setProductDemandId(t.getProductDemandId());
                item.setOwnerId(t.getOwnerId());
                item.setOwner(t.getOwner());
                item.setResourceType(t.getResourceType());
                item.setResourceTime(t.getResourceTime());
                return item;
            }).collect(Collectors.toList()));

            vo.getDemands().add(demandVO);
        }

        return BaseResult.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> saveResourcePlanV2(ResourcePlanV2SaveReq req) {
        Long groupId = req.getProductDemandGroupId();
        Long bizDomainGroupId = req.getBizDomainGroupId();
        String operatorId = req.getOperatorId();
        String operator = req.getOperator();

        if (groupId == null) {
            return BaseResult.fail(400, "产品需求分组ID不能为空");
        }
        if (bizDomainGroupId == null) {
            return BaseResult.fail(400, "业务域集ID不能为空");
        }

        // 1. 获取分组下所有需求ID
        List<ProductDemandGroupItemDO> groupItems = productDemandGroupItemMapper.getByGroupId(groupId);
        if (CollUtil.isEmpty(groupItems)) {
            return BaseResult.success(true);
        }
        Set<Long> demandIds = groupItems.stream()
                .map(ProductDemandGroupItemDO::getProductDemandId)
                .collect(Collectors.toSet());

        // 2. 查询旧的资源分配（用于后续 diff 抄送人）
        List<ProductDemandOwnerTimeDO> oldOwnerTimes = ownerTimeMapper.listByProductDemandIds(demandIds);

        // 3. 删除这些需求的旧资源分配
        ownerTimeMapper.deleteByDemandIds(demandIds, operatorId, operator);

        // 4. 插入新的资源分配
        List<ResourcePlanV2SaveReq.DemandOwnerTimeItem> items = req.getItems();
        if (CollUtil.isNotEmpty(items)) {
            List<ProductDemandOwnerTimeDO> toInsert = items.stream()
                    .filter(item -> demandIds.contains(item.getProductDemandId()))
                    .filter(item -> item.getResourceTime() != null && item.getResourceTime().compareTo(BigDecimal.ZERO) > 0)
                    .map(item -> {
                        ProductDemandOwnerTimeDO doItem = new ProductDemandOwnerTimeDO();
                        doItem.setProductDemandId(item.getProductDemandId());
                        doItem.setOwnerId(item.getOwnerId());
                        doItem.setOwner(item.getOwner());
                        doItem.setResourceType(item.getResourceType());
                        doItem.setResourceTime(item.getResourceTime());
                        doItem.setProductDemandGroupId(groupId);
                        doItem.setBizDomainGroupId(bizDomainGroupId);
                        return doItem;
                    }).collect(Collectors.toList());

            if (CollUtil.isNotEmpty(toInsert)) {
                ownerTimeMapper.batchInsert(toInsert, operatorId, operator);
            }

            // 5. 回写 product_demand 主表的汇总人天（保持兼容）
            syncDemandTimeFromOwnerTime(demandIds, items);
        } else {
            // 清空时也要回写主表
            clearDemandTime(demandIds);
        }

        // 6. 同步抄送人（新增加入抄送、删除移出抄送）
        syncCopiers(oldOwnerTimes, items, demandIds, operatorId, operator);

        return BaseResult.success(true);
    }

    /**
     * 按需求汇总各类型人天，回写 product_demand 主表
     */
    private void syncDemandTimeFromOwnerTime(Set<Long> demandIds,
                                              List<ResourcePlanV2SaveReq.DemandOwnerTimeItem> items) {
        // 按需求ID分组汇总
        Map<Long, Map<String, BigDecimal>> demandTypeSums = new HashMap<>();
        for (ResourcePlanV2SaveReq.DemandOwnerTimeItem item : items) {
            if (!demandIds.contains(item.getProductDemandId())) continue;
            demandTypeSums
                    .computeIfAbsent(item.getProductDemandId(), k -> new HashMap<>())
                    .merge(item.getResourceType(),
                            item.getResourceTime() != null ? item.getResourceTime() : BigDecimal.ZERO,
                            BigDecimal::add);
        }

        List<ProductDemandDO> toUpdate = new ArrayList<>();
        for (Long demandId : demandIds) {
            Map<String, BigDecimal> typeSums = demandTypeSums.getOrDefault(demandId, Collections.emptyMap());
            ProductDemandDO update = new ProductDemandDO();
            update.setId(demandId);
            update.setFrontTime(typeSums.getOrDefault("frontend", BigDecimal.ZERO));
            update.setBackTime(typeSums.getOrDefault("backend", BigDecimal.ZERO));
            update.setQaTime(typeSums.getOrDefault("qa", BigDecimal.ZERO));
            update.setUedTime(typeSums.getOrDefault("ued", BigDecimal.ZERO));
            update.setProductTime(typeSums.getOrDefault("product", BigDecimal.ZERO));
            update.setOpsTime(typeSums.getOrDefault("ops", BigDecimal.ZERO));
            update.setSecurityTime(typeSums.getOrDefault("security", BigDecimal.ZERO));
            update.setTotalTime(typeSums.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            toUpdate.add(update);
        }

        if (CollUtil.isNotEmpty(toUpdate)) {
            productDemandMapper.updateResourceTime(toUpdate);
        }
    }

    private void clearDemandTime(Set<Long> demandIds) {
        List<ProductDemandDO> toUpdate = demandIds.stream().map(id -> {
            ProductDemandDO update = new ProductDemandDO();
            update.setId(id);
            update.setFrontTime(BigDecimal.ZERO);
            update.setBackTime(BigDecimal.ZERO);
            update.setQaTime(BigDecimal.ZERO);
            update.setUedTime(BigDecimal.ZERO);
            update.setProductTime(BigDecimal.ZERO);
            update.setOpsTime(BigDecimal.ZERO);
            update.setSecurityTime(BigDecimal.ZERO);
            update.setTotalTime(BigDecimal.ZERO);
            return update;
        }).collect(Collectors.toList());
        productDemandMapper.updateResourceTime(toUpdate);
    }

    /**
     * 同步抄送人：对比新旧资源分配，新增的负责人加入抄送名单，删除的负责人移出抄送名单。
     * 与老代码 ProductDemandGroupServiceImpl.batchUpsertProductDemandOwners 逻辑一致。
     */
    private void syncCopiers(List<ProductDemandOwnerTimeDO> oldOwnerTimes,
                             List<ResourcePlanV2SaveReq.DemandOwnerTimeItem> newItems,
                             Set<Long> demandIds,
                             String operatorId, String operator) {
        // 旧数据：按需求ID -> 负责人ID集合
        Map<Long, Set<String>> oldDemandOwnerIds = new HashMap<>();
        if (CollUtil.isNotEmpty(oldOwnerTimes)) {
            for (ProductDemandOwnerTimeDO ot : oldOwnerTimes) {
                oldDemandOwnerIds.computeIfAbsent(ot.getProductDemandId(), k -> new HashSet<>())
                        .add(ot.getOwnerId());
            }
        }

        // 新数据：按需求ID -> 负责人信息（去重）
        Map<Long, Map<String, String>> newDemandOwners = new HashMap<>(); // demandId -> (ownerId -> ownerName)
        if (CollUtil.isNotEmpty(newItems)) {
            for (ResourcePlanV2SaveReq.DemandOwnerTimeItem item : newItems) {
                if (!demandIds.contains(item.getProductDemandId())) continue;
                if (item.getResourceTime() == null || item.getResourceTime().compareTo(BigDecimal.ZERO) <= 0) continue;
                newDemandOwners.computeIfAbsent(item.getProductDemandId(), k -> new HashMap<>())
                        .putIfAbsent(item.getOwnerId(), item.getOwner());
            }
        }

        // 收集所有涉及的需求ID，查询需求信息（用于发消息）
        Set<Long> allDemandIds = new HashSet<>(oldDemandOwnerIds.keySet());
        allDemandIds.addAll(newDemandOwners.keySet());
        if (allDemandIds.isEmpty()) return;

        Map<Long, ProductDemandDO> demandMap = productDemandMapper.selectByIdList(allDemandIds).stream()
                .collect(Collectors.toMap(ProductDemandDO::getId, Function.identity(), (e, r) -> e));

        // 处理新增的负责人 → 加入抄送名单
        for (Map.Entry<Long, Map<String, String>> entry : newDemandOwners.entrySet()) {
            Long demandId = entry.getKey();
            Map<String, String> newOwners = entry.getValue();
            Set<String> oldOwners = oldDemandOwnerIds.getOrDefault(demandId, Collections.emptySet());

            // 找出新增的负责人（在新数据中但不在旧数据中）
            Set<PersonAddReq> addedPersons = new LinkedHashSet<>();
            for (Map.Entry<String, String> ownerEntry : newOwners.entrySet()) {
                if (!oldOwners.contains(ownerEntry.getKey())) {
                    addedPersons.add(new PersonAddReq().setUserId(ownerEntry.getKey()).setUserName(ownerEntry.getValue()));
                }
            }

            if (CollUtil.isNotEmpty(addedPersons)) {
                // 查询已有的抄送人，避免重复
                List<PersonDO> existPersons = personComponent.select(demandId, PersonTypeEnum.PRODUCT_DEMAND_CC.getCode(), PersonLevelEnum.CORE.getCode());
                personComponent.add(addedPersons, demandId, PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());

                // 发送消息通知（仅通知真正新增的）
                Set<String> existUserIds = existPersons.stream().map(PersonDO::getUserId).collect(Collectors.toSet());
                List<String> realNewUserIds = addedPersons.stream()
                        .map(PersonAddReq::getUserId)
                        .filter(uid -> !existUserIds.contains(uid))
                        .collect(Collectors.toList());

                ProductDemandDO demand = demandMap.get(demandId);
                if (CollUtil.isNotEmpty(realNewUserIds) && demand != null) {
                    messageEventPublisher.publish(new ProductDemandToCopiedMsgEvent(
                            this, demandId, demand.getCreateMan(), realNewUserIds, demand.getName()));
                }
            }
        }

        // 处理删除的负责人 → 移出抄送名单（仅当该人在新数据中完全不存在时才移除）
        List<Pair<Long, String>> toRemove = new ArrayList<>();
        for (Map.Entry<Long, Set<String>> entry : oldDemandOwnerIds.entrySet()) {
            Long demandId = entry.getKey();
            Set<String> oldOwners = entry.getValue();
            Set<String> newOwnerIds = newDemandOwners.containsKey(demandId)
                    ? newDemandOwners.get(demandId).keySet()
                    : Collections.emptySet();

            for (String oldOwnerId : oldOwners) {
                if (!newOwnerIds.contains(oldOwnerId)) {
                    toRemove.add(Pair.of(demandId, oldOwnerId));
                }
            }
        }

        if (CollUtil.isNotEmpty(toRemove)) {
            PersonRemoveCondition removeCondition = PersonRemoveCondition.builder()
                    .type(PersonTypeEnum.PRODUCT_DEMAND_CC.getCode())
                    .mainId2PersonId(toRemove)
                    .build();
            personComponent.remove(removeCondition, operatorId, operator);
        }
    }

    @Override
    public BaseResult<List<ResourcePlanV2VO.OwnerTimeItem>> getOwnerTimesByDemandId(Long productDemandId) {
        if (productDemandId == null) {
            return BaseResult.success(Collections.emptyList());
        }
        List<ProductDemandOwnerTimeDO> times = ownerTimeMapper.listByProductDemandIds(Collections.singleton(productDemandId));
        List<ResourcePlanV2VO.OwnerTimeItem> items = times.stream().map(t -> {
            ResourcePlanV2VO.OwnerTimeItem item = new ResourcePlanV2VO.OwnerTimeItem();
            item.setId(t.getId());
            item.setProductDemandId(t.getProductDemandId());
            item.setOwnerId(t.getOwnerId());
            item.setOwner(t.getOwner());
            item.setResourceType(t.getResourceType());
            item.setResourceTime(t.getResourceTime());
            return item;
        }).collect(Collectors.toList());
        return BaseResult.success(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> saveOwnerTimesByDemandId(ResourcePlanV2SaveReq req) {
        List<ResourcePlanV2SaveReq.DemandOwnerTimeItem> items = req.getItems();
        String operatorId = req.getOperatorId();
        String operator = req.getOperator();

        // 从 items 中提取需求ID（单条需求场景下所有 item 的 productDemandId 相同）
        Set<Long> demandIds = new HashSet<>();
        if (CollUtil.isNotEmpty(items)) {
            items.forEach(item -> demandIds.add(item.getProductDemandId()));
        }
        if (demandIds.isEmpty()) {
            return BaseResult.success(true);
        }

        // 查询旧的资源分配（用于后续 diff 抄送人）
        List<ProductDemandOwnerTimeDO> oldOwnerTimes = ownerTimeMapper.listByProductDemandIds(demandIds);

        // 删除旧数据
        ownerTimeMapper.deleteByDemandIds(demandIds, operatorId, operator);

        // 插入新数据
        if (CollUtil.isNotEmpty(items)) {
            List<ProductDemandOwnerTimeDO> toInsert = items.stream()
                    .filter(item -> item.getResourceTime() != null && item.getResourceTime().compareTo(BigDecimal.ZERO) > 0)
                    .map(item -> {
                        ProductDemandOwnerTimeDO doItem = new ProductDemandOwnerTimeDO();
                        doItem.setProductDemandId(item.getProductDemandId());
                        doItem.setOwnerId(item.getOwnerId());
                        doItem.setOwner(item.getOwner());
                        doItem.setResourceType(item.getResourceType());
                        doItem.setResourceTime(item.getResourceTime());
                        // 单条需求场景不需要 groupId
                        return doItem;
                    }).collect(Collectors.toList());

            if (CollUtil.isNotEmpty(toInsert)) {
                ownerTimeMapper.batchInsert(toInsert, operatorId, operator);
            }

            // 回写主表汇总
            syncDemandTimeFromOwnerTime(demandIds, items);
        } else {
            clearDemandTime(demandIds);
        }

        // 同步抄送人（新增加入抄送、删除移出抄送）
        syncCopiers(oldOwnerTimes, items, demandIds, operatorId, operator);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Map<Long, Map<String, BigDecimal>>> getGroupTimeSummary(List<Long> groupIds) {
        if (CollUtil.isEmpty(groupIds)) {
            return BaseResult.success(Collections.emptyMap());
        }
        List<Map<String, Object>> rows = ownerTimeMapper.sumByGroupIds(groupIds);
        Map<Long, Map<String, BigDecimal>> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long groupId = ((Number) row.get("groupId")).longValue();
            String resourceType = (String) row.get("resourceType");
            BigDecimal totalTime = new BigDecimal(row.get("totalTime").toString());
            result.computeIfAbsent(groupId, k -> new HashMap<>())
                    .put(resourceType, totalTime);
        }
        return BaseResult.success(result);
    }
}
