package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandGroupCondition;
import com.timevale.forward.dal.condition.BizDemandGroupQueryCondition;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.BizGroupCondition;
import com.timevale.forward.dal.condition.ProductDemandGroupCondition;
import com.timevale.forward.dal.condition.ProductDemandGroupQueryCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.ProductGroupCondition;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDemandGroupFieldDO;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.dal.entity.ProductDemandGroupFieldDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.DynamicGroupService;
import com.timevale.forward.facade.api.query.BizDemandGroupList;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.DynamicBizDemandGroupList;
import com.timevale.forward.facade.api.query.DynamicProductDemandGroupList;
import com.timevale.forward.facade.api.query.ProductDemandGroupList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.query.ViewsGroupQueryList;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.DemandGroupNodeVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.model.enums.BizDemandGroupFieldEnum;
import com.timevale.forward.model.enums.BizDemandSelectFieldEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProductDemandTypeEnum;
import com.timevale.forward.model.enums.ProductGroupFieldEnum;
import com.timevale.forward.model.enums.ProductSelectFieldEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.LabelComponent;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.duplicate.GroupDuplicateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @auther: yuhua
 * @date: 2025/7/7 10:02
 * @description: 动态分组实现
 */
@Slf4j
@LogPoint
@RestService
public class DynamicGroupServiceImpl implements DynamicGroupService {

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private LabelComponent labelComponent;

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private BizLabelMapper bizLabelMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private ProductDemandComponent productDemandComponent;

    @Resource
    private GroupDuplicateUtil groupDuplicateUtil;

    @Resource
    private BizDemandComponent bizDemandComponent;

    private static final Map<String, String> PRODUCT_GROUP_FIELD_MAP = new HashMap<>();

    private static final Map<String, String> BIZ_GROUP_FIELD_MAP = new HashMap<>();

    private static final Map<String, Function<ProductDemandGroupFieldDO, Object>> FUNCTION_FIELD_MAP = new HashMap<>();

    private static final Map<String, Function<BizDemandGroupFieldDO, Object>> BIZ_FUNCTION_FIELD_MAP = new HashMap<>();

    private static final Map<String, String> QUERY_OTHER_FIELD_MAP = new HashMap<>();

    private static final String OTHER = "其他";

    static {
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField(), "bizDomainId");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.PRODUCT_LINE.getGroupField(), "productLineId");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField(), "labelId");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.TYPE.getGroupField(), "type");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.STATUS.getGroupField(), "status");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.PRIORITY.getGroupField(), "priority");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.EXPECT_SCHEDULE_TIME.getGroupField(), "expectScheduleTime");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.OWNER.getGroupField(), "ownerId");
    }

    static {
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField(), "bizDomainId");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField(), "productLineId");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.LABEL.getGroupField(), "labelId");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.TARGET_CUSTOMER.getGroupField(), "targetCustomer");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.STATUS.getGroupField(), "status");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.PRIORITY.getGroupField(), "priority");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.DEMAND_DEPT.getGroupField(), "deptId");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField(), "receiveManId");
    }

    static {
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField(), ProductDemandGroupFieldDO::getBizDomainId);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.PRODUCT_LINE.getGroupField(), ProductDemandGroupFieldDO::getProductLineId);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField(), ProductDemandGroupFieldDO::getLabelId);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.TYPE.getGroupField(), ProductDemandGroupFieldDO::getType);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.STATUS.getGroupField(), ProductDemandGroupFieldDO::getStatus);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.PRIORITY.getGroupField(), ProductDemandGroupFieldDO::getPriority);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.EXPECT_SCHEDULE_TIME.getGroupField(), ProductDemandGroupFieldDO::getExpectScheduleTime);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.OWNER.getGroupField(), ProductDemandGroupFieldDO::getOwnerId);
    }

    static {
        BIZ_FUNCTION_FIELD_MAP.put(BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField(), BizDemandGroupFieldDO::getBizDomainId);
        BIZ_FUNCTION_FIELD_MAP.put(BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField(), BizDemandGroupFieldDO::getProductLineId);
        BIZ_FUNCTION_FIELD_MAP.put(BizDemandGroupFieldEnum.LABEL.getGroupField(), BizDemandGroupFieldDO::getLabelId);
        BIZ_FUNCTION_FIELD_MAP.put(BizDemandGroupFieldEnum.TARGET_CUSTOMER.getGroupField(), BizDemandGroupFieldDO::getTargetCustomer);
        BIZ_FUNCTION_FIELD_MAP.put(BizDemandGroupFieldEnum.STATUS.getGroupField(), BizDemandGroupFieldDO::getStatus);
        BIZ_FUNCTION_FIELD_MAP.put(BizDemandGroupFieldEnum.PRIORITY.getGroupField(), BizDemandGroupFieldDO::getPriority);
        BIZ_FUNCTION_FIELD_MAP.put(BizDemandGroupFieldEnum.DEMAND_DEPT.getGroupField(), BizDemandGroupFieldDO::getDeptId);
        BIZ_FUNCTION_FIELD_MAP.put(BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField(), BizDemandGroupFieldDO::getReceiveManId);
    }

    static {
        QUERY_OTHER_FIELD_MAP.put("labelId", "notInLabelIds");
        QUERY_OTHER_FIELD_MAP.put("ownerId", "notInOwnerIds");
        QUERY_OTHER_FIELD_MAP.put("receiveManId", "notInReceiveManIds");
    }

    @Override
    public BaseResult<List<DemandGroupNodeVO>> getProductDemandsGroupTree(DynamicProductDemandGroupList dynamicGroupQueryList) {
        List<ViewsGroupQueryList> groupFields = dynamicGroupQueryList.getGroupFields();
        if (CollUtil.isEmpty(groupFields)) {
            throw new BaseBizRuntimeException("分组字段不能为空");
        }

        // 1) 构建基础查询条件（复用一层分组的参数处理逻辑）
        ProductDemandQueryList productDemandQueryList = dynamicGroupQueryList.getFilters();
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        ProductDemandGroupCondition parentCondition = ProductDemandGroupCondition.builder().build();

        if (groupDuplicateUtil.setOwnerIdByAscription(productDemandQueryList, userInfo, condition, innerUserPersonClient)) {
            return BaseResult.success(new ArrayList<>());
        }

        // 标签过滤
        if (CollectionUtils.isNotEmpty(productDemandQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(productDemandQueryList.getLabelCategoryIds())) {
            Boolean containLabel = productDemandQueryList.getContainLabel();
            List<Long> newLabelIds = labelComponent.getLabelIds(productDemandQueryList.getLabelIds(), productDemandQueryList.getLabelCategoryIds());
            if (CollectionUtils.isEmpty(newLabelIds) && Boolean.TRUE.equals(containLabel)) {
                return BaseResult.success(Collections.emptyList());
            }

            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (Boolean.TRUE.equals(containLabel)) {
                if (CollectionUtils.isEmpty(bizIds)) {
                    return BaseResult.success(Collections.emptyList());
                }
                condition.setInProductDemandIds(bizIds);
            } else {
                condition.setNotInProductDemandIds(bizIds);
            }
        }

        // 3) 名称字典一次性查询
        Map<Long, String> bizDomainNameMap = Collections.emptyMap();
        Map<Long, String> productLineNameMap = Collections.emptyMap();
        Map<String, String> ownerNameMap = Collections.emptyMap();
        Map<Long, String> labelNameMap = Collections.emptyMap();

        long count = groupFields.stream().filter(groupField -> groupField.getType() == 1).count();
        if (count > 1) {
            List<Long> labelCategoryId = groupFields.stream()
                    .filter(groupField -> groupField.getType() == 1)
                    .map(groupField -> Long.valueOf(groupField.getKey()))
                    .collect(Collectors.toList());
            parentCondition.setLabelCategoryIds(labelCategoryId);
            ProductDemandGroupQueryCondition groupCondition = ProductDemandGroupQueryCondition.builder()
                    .condition(condition)
                    .parentCondition(parentCondition)
                    .orderField(dynamicGroupQueryList.getOrderField())
                    .build();
            List<ProductDemandGroupFieldDO> simpleGroupList = productDemandComponent.getSimpleGroupList(groupCondition);
            if (CollUtil.isEmpty(simpleGroupList)) {
                return BaseResult.success(new ArrayList<>());
            }

            List<String> groupFieldKeys = groupFields.stream().map(ViewsGroupQueryList::getKey).collect(Collectors.toList());

            if (groupFieldKeys.contains(ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField())) {
                Set<Long> ids = extractUniqueIds(simpleGroupList, ProductDemandGroupFieldDO::getBizDomainId);
                if (CollectionUtils.isNotEmpty(ids)) {
                    bizDomainNameMap = bizDomainMapper.getByIds(ids).stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
                }
            }
            if (groupFieldKeys.contains(ProductGroupFieldEnum.PRODUCT_LINE.getGroupField())) {
                Set<Long> productLineIds = extractUniqueIds(simpleGroupList, ProductDemandGroupFieldDO::getProductLineId);
                if (CollectionUtils.isNotEmpty(productLineIds)) {
                    productLineNameMap = productLineMapper.getByIds(productLineIds).stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
                }
            }
            if (groupFieldKeys.contains(ProductGroupFieldEnum.OWNER.getGroupField())) {
                ownerNameMap = getPersonMap(ownerNameMap, simpleGroupList.stream().map(ProductDemandGroupFieldDO::getOwnerId));
            }

            // 定义分组条件
            List<LabelDO> byCategoryIds = labelMapper.getByCategoryIds(labelCategoryId, false);
            // 根据不同的标签类别，得到以分组id为key,不同的标签Map集合的Map集合，key为标签id，value为标签名称
            Map<Long, Map<Long, String>> labelMaps = byCategoryIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(LabelDO::getLabelCategoryId,
                            Collectors.toMap(LabelDO::getId, LabelDO::getName, (a, b) -> b)));

            List<ProductGroupCondition> conditions = new ArrayList<>(5);

            for (int i = 0; i < groupFields.size(); i++) {
                if (groupFields.get(i).getType() == 1) {
                    conditions.add(new ProductGroupCondition(ProductGroupFieldEnum.LABEL.getGroupField() + i, ProductDemandGroupFieldDO::getLabelId,
                            labelMaps.get(Long.valueOf(groupFields.get(i).getKey()))));
                } else {
                    conditions.add(new ProductGroupCondition(groupFields.get(i).getKey(), FUNCTION_FIELD_MAP.get(groupFields.get(i).getKey()), null));
                }
            }
            Map<String, Object> groupResult = group(simpleGroupList, conditions);
            // 过滤空的叶子节点
//            filterEmptyLeafNodesInResult(groupResult);
            // 转换方法
            List<DemandGroupNodeVO> treeNodes = transformToTree(groupResult, 0, conditions, bizDomainNameMap, productLineNameMap, ownerNameMap);

            // 排序（各层按 label 升序）
            sortTreeByLabel(treeNodes);
            return BaseResult.success(treeNodes);
        } else {
            for (ViewsGroupQueryList groupField : groupFields) {
                if (groupField.getType() == 1) {
                    parentCondition.setLabelCategoryIds(Collections.singletonList(Long.valueOf(groupField.getKey())));
                    groupField.setKey(ProductGroupFieldEnum.LABEL.getGroupField());
                }
            }
        }
        // 2) 组织一次性聚合 SQL 的 selectField 与 groupField
        String selectField = buildSelectField(groupFields);
        String groupField = buildGroupField(groupFields);
        List<String> groupFieldKeys = groupFields.stream().map(ViewsGroupQueryList::getKey).collect(Collectors.toList());

        ProductDemandGroupQueryCondition groupCondition = ProductDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .selectField(selectField)
                .groupField(groupField)
                .orderField(dynamicGroupQueryList.getOrderField())
                .build();

        List<ProductDemandGroupFieldDO> rows = productDemandComponent.getGroupTree(groupCondition);
        if (CollectionUtils.isEmpty(rows)) {
            return BaseResult.success(Collections.emptyList());
        }

        if (groupFieldKeys.contains(ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField())) {
            Set<Long> ids = extractUniqueIds(rows, ProductDemandGroupFieldDO::getBizDomainId);
            if (CollectionUtils.isNotEmpty(ids)) {
                bizDomainNameMap = bizDomainMapper.getByIds(ids).stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
            }
        }
        if (groupFieldKeys.contains(ProductGroupFieldEnum.PRODUCT_LINE.getGroupField())) {
            Set<Long> productLineIds = extractUniqueIds(rows, ProductDemandGroupFieldDO::getProductLineId);
            if (CollectionUtils.isNotEmpty(productLineIds)) {
                productLineNameMap = productLineMapper.getByIds(productLineIds).stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
            }
        }
        if (groupFieldKeys.contains(ProductGroupFieldEnum.OWNER.getGroupField())) {
            ownerNameMap = getPersonMap(ownerNameMap, rows.stream().map(ProductDemandGroupFieldDO::getOwnerId));
        }
        if (groupFieldKeys.contains(ProductGroupFieldEnum.LABEL.getGroupField())) {
            labelNameMap = getLabelMap(labelNameMap, rows.stream().map(ProductDemandGroupFieldDO::getLabelId));
        }

        // 4) 构建树（一次性结果 → 树），并处理 type 维度的多值拆分
        List<DemandGroupNodeVO> roots = new ArrayList<>();
        // key: path
        Map<String, DemandGroupNodeVO> levelNodeMap = new HashMap<>();

        // 聚合计数（用于纠正 type 维度导致的父层去重统计）
        List<Map<String, Long>> prefixTotals = new ArrayList<>();
        for (int i = 0; i < groupFields.size(); i++) {
            prefixTotals.add(new HashMap<>());
        }

        // 先构建 prefixTotals（不拆分 type）
        for (ProductDemandGroupFieldDO row : rows) {
            for (int level = 0; level < groupFieldKeys.size(); level++) {
                String prefixKey = buildPrefixKey(groupFieldKeys, row, level);
                prefixTotals.get(level).merge(prefixKey, Optional.ofNullable(row.getTotal()).orElse(0L), Long::sum);
            }
        }

        int typeIndex = groupFieldKeys.indexOf(ProductGroupFieldEnum.TYPE.getGroupField());

        // 构建节点（对 type 维度进行拆分）
        for (ProductDemandGroupFieldDO row : rows) {
            buildNodesForRow(groupFieldKeys, row, roots, levelNodeMap,
                    bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap);
        }

        // 5) 计算合计：
        // 先为叶子节点赋值（叶子已经在构建时累加 total），然后从底向上汇总。
        computeTotalsBottomUp(roots);

        // 若包含 type 维度，则对 type 上层的节点用 prefixTotals 进行去重修正（避免被子层的多值重复计数放大）
        if (typeIndex >= 0) {
            applyPrefixTotalsCorrection(roots, groupFieldKeys, prefixTotals, typeIndex);
        }

        // 排序（各层按 label 升序）
        sortTreeByLabel(roots);

        return BaseResult.success(roots);
    }

    /**
     * 执行多级分组
     *
     * @param data
     * @param conditions
     * @return
     */
    public Map<String, Object> group(List<ProductDemandGroupFieldDO> data,
                                     List<ProductGroupCondition> conditions) {
        // 构建动态Collector链
        Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>> collector =
                buildCollectorChain(conditions, 0);

        // 执行分组
        return data.stream().collect(collector);
    }

    public Map<String, Object> bizDemandGroup(List<BizDemandGroupFieldDO> data,
                                     List<BizGroupCondition> conditions) {
        // 构建动态Collector链
        Collector<BizDemandGroupFieldDO, ?, Map<String, Object>> collector =
                buildBizDemandCollectorChain(conditions, 0);

        // 执行分组
        return data.stream().collect(collector);
    }

    private Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>> buildCollectorChain(
            List<ProductGroupCondition> conditions, int index) {

        if (index >= conditions.size()) {
            // 终止条件：返回计数Collector，包装成Map
            return Collector.of(
                    HashMap::new,
                    (map, item) -> map.put("total", (Long) map.getOrDefault("total", 0L) + 1L),
                    (map1, map2) -> {
                        map1.put("total", (Long) map1.getOrDefault("total", 0L) +
                                (Long) map2.getOrDefault("total", 0L));
                        return map1;
                    },
                    Collector.Characteristics.IDENTITY_FINISH
            );
        }

        ProductGroupCondition condition = conditions.get(index);
        Function<ProductDemandGroupFieldDO, String> extractor = item -> {
            Object rawValue = condition.getFieldExtractor().apply(item);
            return condition.getValueMapping() != null
                    ? condition.getValueMapping().getOrDefault(rawValue, OTHER)
                    : String.valueOf(rawValue);
        };

        // 递归构建下一级分组器
        Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>> nextCollector =
                buildCollectorChain(conditions, index + 1);

        // 使用显式类型声明解决类型推断问题
        Collector<ProductDemandGroupFieldDO, ?, Map<String, Map<String, Object>>> groupingCollector =
                Collectors.groupingBy(
                        extractor,
                        LinkedHashMap::new,
                        (Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>>) nextCollector
                );

        // 添加类型转换确保最终返回类型一致
        return (Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>>) (Collector<?, ?, ?>) groupingCollector;
    }

    // 新增方法用于过滤结果中的空叶子节点
    private void filterEmptyLeafNodesInResult(Map<String, Object> map) {
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) value;

                // 检查是否是叶子节点（只包含total键且值为0）
                if (subMap.size() == 1 && subMap.containsKey("total") &&
                        subMap.get("total") instanceof Long) {
                    // 如果是空的叶子节点，移除它
                    iterator.remove();
                } else {
                    // 递归处理子节点
                    filterEmptyLeafNodesInResult(subMap);
                    // 如果处理后子map为空，则移除该节点
                    if (subMap.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private Collector<BizDemandGroupFieldDO, ?, Map<String, Object>> buildBizDemandCollectorChain(
            List<BizGroupCondition> conditions, int index) {

        if (index >= conditions.size()) {
            // 终止条件：返回计数Collector，包装成Map
            return Collector.of(
                    HashMap::new,
                    (map, item) -> map.put("total", (Long) map.getOrDefault("total", 0L) + 1L),
                    (map1, map2) -> {
                        map1.put("total", (Long) map1.getOrDefault("total", 0L) +
                                (Long) map2.getOrDefault("total", 0L));
                        return map1;
                    },
                    Collector.Characteristics.IDENTITY_FINISH
            );
        }

        BizGroupCondition condition = conditions.get(index);
        Function<BizDemandGroupFieldDO, String> extractor = item -> {
            Object rawValue = condition.getFieldExtractor().apply(item);
            return condition.getValueMapping() != null
                    ? condition.getValueMapping().getOrDefault(rawValue, OTHER)
                    : String.valueOf(rawValue);
        };

        // 递归构建下一级分组器
        Collector<BizDemandGroupFieldDO, ?, Map<String, Object>> nextCollector =
                buildBizDemandCollectorChain(conditions, index + 1);

        // 使用显式类型声明解决类型推断问题
        Collector<BizDemandGroupFieldDO, ?, Map<String, Map<String, Object>>> groupingCollector =
                Collectors.groupingBy(
                        extractor,
                        LinkedHashMap::new,
                        (Collector<BizDemandGroupFieldDO, ?, Map<String, Object>>) nextCollector
                );

        // 添加类型转换确保最终返回类型一致
        return (Collector<BizDemandGroupFieldDO, ?, Map<String, Object>>) (Collector<?, ?, ?>) groupingCollector;
    }

    /**
     * 将嵌套Map结构的分组结果转换为树形结构
     *
     * @param mapResult
     * @param currentLevel
     * @return 返回结果
     */
    public List<DemandGroupNodeVO> transformToTree(Map<String, Object> mapResult, int currentLevel, List<ProductGroupCondition> conditions,
                                                   Map<Long, String> bizDomainNameMap,
                                                   Map<Long, String> productLineNameMap,
                                                   Map<String, String> ownerNameMap) {
        List<DemandGroupNodeVO> nodes = new ArrayList<>();
        // 递归结束条件
        if (currentLevel >= conditions.size()) {
            return nodes;
        }
        ProductGroupCondition productGroupCondition = conditions.get(currentLevel);
        for (Map.Entry<String, Object> entry : mapResult.entrySet()) {
            String field = productGroupCondition.getFieldName();
            String fieldValue = entry.getKey();
            String label = entry.getKey();
            if (field.startsWith(ProductGroupFieldEnum.LABEL.getGroupField())) {
                field = ProductGroupFieldEnum.LABEL.getGroupField();
                // 找到以entry.getKey()为值的label
                if (productGroupCondition.getValueMapping() != null) {
                    Map<String, Long> flipMap = flipMap(productGroupCondition.getValueMapping());
                    fieldValue = flipMap.get(label) != null ? String.valueOf(flipMap.get(label)) : OTHER;
                }
            }
            if (ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(field)) {
                label = bizDomainNameMap.get(Long.parseLong(fieldValue));
            }
            if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(field)) {
                label = productLineNameMap.get(Long.parseLong(fieldValue));
            }
            if (ProductGroupFieldEnum.OWNER.getGroupField().equals(field)) {
                label = ownerNameMap.get(fieldValue);
            }
            Object value = entry.getValue();

            DemandGroupNodeVO node = new DemandGroupNodeVO();
            node.setField(PRODUCT_GROUP_FIELD_MAP.get(field));
            node.setFieldValue(fieldValue);
            node.setLabel(label);
            node.setTotal(0L);

            if (value instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) value;
                // 检查是否是叶子节点（包含total键）
                if (valueMap.containsKey("total")) {
                    // 叶子节点（直接设置count）
                    Object totalValue = valueMap.get("total");
                    node.setTotal(totalValue instanceof Number ? ((Number) totalValue).longValue() : 0L);
                } else {
                    // 递归处理子层级
                    List<DemandGroupNodeVO> childNodes = transformToTree(valueMap, currentLevel + 1, conditions, bizDomainNameMap, productLineNameMap, ownerNameMap);
                    node.setChildren(childNodes);
                    // 计算当前节点count（子节点count之和）
                    node.setTotal(childNodes.stream().mapToLong(DemandGroupNodeVO::getTotal).sum());
                }
            }
            nodes.add(node);
        }
        return nodes;
    }

    // 判断是否应该包含"其他"节点
    private boolean shouldIncludeOtherNode(Object value) {
        if (value instanceof Map) {
            Map<String, Object> valueMap = (Map<String, Object>) value;
            // 如果map中只包含total键，则说明是叶子节点，不需要"其他"分类
            return !valueMap.containsKey("total") || valueMap.size() > 1;
        }
        return true;
    }

    public List<DemandGroupNodeVO> bizDemandTransformToTree(Map<String, Object> mapResult,
                                                            int currentLevel,
                                                            List<BizGroupCondition> conditions,
                                                            Map<Long, String> bizDomainNameMap,
                                                            Map<Long, String> productLineNameMap,
                                                            Map<String, String> deptNameMap,
                                                            Map<String, String> receiveManNameMap) {
        List<DemandGroupNodeVO> nodes = new ArrayList<>();
        // 递归结束条件
        if (currentLevel >= conditions.size()) {
            return nodes;
        }
        BizGroupCondition bizGroupCondition = conditions.get(currentLevel);
        for (Map.Entry<String, Object> entry : mapResult.entrySet()) {
            String field = bizGroupCondition.getFieldName();
            String fieldValue = entry.getKey();
            String label = entry.getKey();
            if (field.startsWith(BizDemandGroupFieldEnum.LABEL.getGroupField())) {
                field = BizDemandGroupFieldEnum.LABEL.getGroupField();
                // 找到以entry.getKey()为值的label
                if (bizGroupCondition.getValueMapping() != null) {
                    Map<String, Long> flipMap = flipMap(bizGroupCondition.getValueMapping());
                    fieldValue = flipMap.get(label) != null ? String.valueOf(flipMap.get(label)) : OTHER;
                }
            }
            if (BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(field)) {
                label = bizDomainNameMap.get(Long.parseLong(fieldValue));
            }
            if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(field)) {
                label = productLineNameMap.get(Long.parseLong(fieldValue));
            }
            if (BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField().equals(field)) {
                label = receiveManNameMap.get(fieldValue);
            }
            if (BizDemandGroupFieldEnum.DEMAND_DEPT.getGroupField().equals(field)) {
                label = deptNameMap.get(fieldValue);
            }
            Object value = entry.getValue();

            DemandGroupNodeVO node = new DemandGroupNodeVO();
            node.setField(PRODUCT_GROUP_FIELD_MAP.get(field));
            node.setLabel(label);
            node.setFieldValue(fieldValue);
            node.setTotal(0L);

            if (value instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) value;
                // 检查是否是叶子节点（包含total键）
                if (valueMap.containsKey("total")) {
                    // 叶子节点（直接设置count）
                    Object totalValue = valueMap.get("total");
                    node.setTotal(totalValue instanceof Number ? ((Number) totalValue).longValue() : 0L);
                } else {
                    // 递归处理子层级
                    List<DemandGroupNodeVO> childNodes = bizDemandTransformToTree(valueMap, currentLevel + 1, conditions, bizDomainNameMap, productLineNameMap, deptNameMap, receiveManNameMap);
                    node.setChildren(childNodes);
                    // 计算当前节点count（子节点count之和）
                    node.setTotal(childNodes.stream().mapToLong(DemandGroupNodeVO::getTotal).sum());
                }
            }
            nodes.add(node);
        }
        return nodes;
    }

    public static <K, V> Map<V, K> flipMap(Map<K, V> originalMap) {
        return originalMap.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getValue,
                        Map.Entry::getKey,
                        // 处理重复value的情况
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 提取去重 ID 集合的通用方法
     *
     * @param rows   数据列表
     * @param mapper 从对象中提取 ID 的函数
     * @param <T>    数据对象类型
     * @return 去重后的 ID 集合
     */
    private <T> Set<Long> extractUniqueIds(List<T> rows, Function<T, Long> mapper) {
        return rows.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }


    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> getProductDemandList(DynamicProductDemandGroupList dynamicGroupQueryList) {
        if (Objects.isNull(dynamicGroupQueryList.getGroupFilters())) {
            throw new BaseBizRuntimeException("父分组查询条件不能为空");
        }

        ProductDemandQueryList productDemandQueryList = dynamicGroupQueryList.getFilters();
        ProductDemandGroupList parentProductDemandQueryList = dynamicGroupQueryList.getGroupFilters();
        List<ViewsGroupQueryList> groupFields = dynamicGroupQueryList.getGroupFields();
        log.info("产品需求接收参数:{}", dynamicGroupQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        ProductDemandGroupCondition parentCondition = ProductDemandCopier.INSTANCE.convert(parentProductDemandQueryList);

        if (groupDuplicateUtil.setOwnerIdByAscription(productDemandQueryList, userInfo, condition, innerUserPersonClient)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        //是否打标
        if (CollectionUtils.isNotEmpty(productDemandQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(productDemandQueryList.getLabelCategoryIds())) {
            Boolean containLabel = productDemandQueryList.getContainLabel();

            List<Long> newLabelIds = labelComponent.getLabelIds(productDemandQueryList.getLabelIds(), productDemandQueryList.getLabelCategoryIds());

            // 查询包含且类别下没有标签
            if (CollectionUtils.isEmpty(newLabelIds) && Boolean.TRUE.equals(containLabel)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }

            // 查询使用这些标签的需求id
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (Boolean.TRUE.equals(containLabel)) {
                if (CollectionUtils.isEmpty(bizIds)) {
                    return BaseResult.success(ResultUtil.pageEmpty());
                }
                condition.setInProductDemandIds(bizIds);
            } else {
                condition.setNotInProductDemandIds(bizIds);
            }

        }

        // 获取标签类别Id
        List<Long> labelCategoryIds = groupFields.stream().filter(field -> field.getType() == 1).map(field -> Long.valueOf(field.getKey())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(labelCategoryIds)) {
            List<Long> productDemandIds = new ArrayList<>();
            //查询等于标签id的需求
            List<Long> labelIds = parentCondition.getLabelIds();
            if (CollUtil.isNotEmpty(labelIds)) {
                List<Long> newLabelIds = labelComponent.getLabelIds(labelIds, labelCategoryIds);
                // 查询使用这些标签的需求id
                List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
                List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

                if (CollectionUtils.isEmpty(bizIds)) {
                    return BaseResult.success(ResultUtil.pageEmpty());
                }
                productDemandIds.addAll(bizIds);
            }

            //查询不等于标签id的需求
            List<Long> notInLabelIds = parentCondition.getNotInLabelIds();
            if (CollUtil.isNotEmpty(notInLabelIds)) {
                List<Long> ids = labelMapper.getByCategoryIds(labelCategoryIds, false).stream().map(LabelDO::getId).collect(Collectors.toList());
                ids.removeAll(notInLabelIds);
                // 查询使用这些标签的需求id
                List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(ids, BizTypeEnum.PRODUCT_DEMAND.getCode());
                List<Long> notInBizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(notInBizIds)) {
                    productDemandIds.addAll(notInBizIds);
                }
            }
            parentCondition.setInProductDemandIds(productDemandIds);
        }

        ProductDemandGroupQueryCondition groupCondition = ProductDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .groupField(null)
                .orderField(dynamicGroupQueryList.getOrderField())
                .build();

        List<ProductDemandListDO> productDemandListDO = productDemandComponent.getGroupList(groupCondition);

        PageQueryResult<ProductDemandVO> pageQueryResult = groupDuplicateUtil.getDemandVOQueryResultVO(productDemandListDO);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<DemandGroupNodeVO>> getBizDemandsGroupTree(DynamicBizDemandGroupList dynamicGroupQueryList) {

        List<ViewsGroupQueryList> groupFields = dynamicGroupQueryList.getGroupFields();
        if (CollUtil.isEmpty(groupFields)) {
            throw new BaseBizRuntimeException("分组字段不能为空");
        }

        BizDemandQueryList bizDemandQueryList = dynamicGroupQueryList.getFilters();
        // 转换查询条件
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);
        BizDemandGroupCondition parentCondition = BizDemandGroupCondition.builder().build();

        boolean resultIsEmpty = groupDuplicateUtil.isResultIsEmpty(bizDemandQueryList, condition);
        if (resultIsEmpty) {
            return BaseResult.success(new ArrayList<>());
        }

        // 标签
        if (CollectionUtils.isNotEmpty(bizDemandQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(bizDemandQueryList.getLabelCategoryIds())) {
            List<Long> labelIds = labelComponent.getLabelIds(bizDemandQueryList.getLabelIds(), bizDemandQueryList.getLabelCategoryIds());
            if (CollectionUtils.isEmpty(labelIds) && bizDemandQueryList.getContainLabel()) {
                return BaseResult.success(new ArrayList<>());
            }
            condition.setLabelIds(labelIds);
        }

        // 3) 名称字典一次性查询
        Map<String, String> deptNameMap = Collections.emptyMap();
        Map<Long, String> bizDomainNameMap = Collections.emptyMap();
        Map<Long, String> productLineNameMap = Collections.emptyMap();
        Map<String, String> receiveManNameMap = Collections.emptyMap();
        Map<Long, String> labelNameMap = Collections.emptyMap();

        long count = groupFields.stream().filter(groupField -> groupField.getType() == 1).count();
        if (count > 1) {
            List<Long> labelCategoryId = groupFields.stream()
                    .filter(groupField -> groupField.getType() == 1)
                    .map(groupField -> Long.valueOf(groupField.getKey()))
                    .collect(Collectors.toList());
            parentCondition.setLabelCategoryIds(labelCategoryId);
            BizDemandGroupQueryCondition groupCondition = BizDemandGroupQueryCondition.builder()
                    .condition(condition)
                    .parentCondition(parentCondition)
                    .orderField(dynamicGroupQueryList.getOrderField())
                    .build();
            List<BizDemandGroupFieldDO> simpleGroupList = bizDemandComponent.getSimpleGroupList(groupCondition);
            if (CollUtil.isEmpty(simpleGroupList)) {
                return BaseResult.success(new ArrayList<>());
            }

            List<String> bizGroupFieldKeys = groupFields.stream().map(ViewsGroupQueryList::getKey).collect(Collectors.toList());

            if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField())) {
                Set<Long> ids = extractUniqueIds(simpleGroupList, BizDemandGroupFieldDO::getBizDomainId);
                if (CollectionUtils.isNotEmpty(ids)) {
                    bizDomainNameMap = bizDomainMapper.getByIds(ids).stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
                }
            }
            if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField())) {
                Set<Long> productLineIds = extractUniqueIds(simpleGroupList, BizDemandGroupFieldDO::getProductLineId);
                if (CollectionUtils.isNotEmpty(productLineIds)) {
                    productLineNameMap = productLineMapper.getByIds(productLineIds).stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
                }
            }
            if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField())) {
                receiveManNameMap = getPersonMap(receiveManNameMap, simpleGroupList.stream().map(BizDemandGroupFieldDO::getReceiveManId));
            }

            if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.DEMAND_DEPT.getGroupField())) {
                Set<Long> deptIds = simpleGroupList.stream().map(BizDemandGroupFieldDO::getDeptId).collect(Collectors.toSet());
                if (CollectionUtils.isNotEmpty(deptIds)) {
                    Map<Long, GroupResponse> groupListTreeMap = bizDemandComponent.getGroupListTreeMap(deptIds);
                    Map<String, String> map = new HashMap<>();
                    Collection<GroupResponse> deptValues = groupListTreeMap.values();
                    if (CollectionUtils.isNotEmpty(deptValues)) {
                        for (GroupResponse r : deptValues) {
                            map.put(r.getGroupId(), r.getGroupName());
                        }
                    }
                    deptNameMap = map;
                }
            }

            // 定义分组条件
            List<LabelDO> byCategoryIds = labelMapper.getByCategoryIds(labelCategoryId, false);
            // 根据不同的标签类别，得到以分组id为key,不同的标签Map集合的Map集合，key为标签id，value为标签名称
            Map<Long, Map<Long, String>> labelMaps = byCategoryIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(LabelDO::getLabelCategoryId,
                            Collectors.toMap(LabelDO::getId, LabelDO::getName, (a, b) -> b)));

            List<BizGroupCondition> conditions = new ArrayList<>(5);

            for (int i = 0; i < groupFields.size(); i++) {
                if (groupFields.get(i).getType() == 1) {
                    conditions.add(new BizGroupCondition(BizDemandGroupFieldEnum.LABEL.getGroupField() + i, BizDemandGroupFieldDO::getLabelId,
                            labelMaps.get(Long.valueOf(groupFields.get(i).getKey()))));
                } else {
                    conditions.add(new BizGroupCondition(groupFields.get(i).getKey(), BIZ_FUNCTION_FIELD_MAP.get(groupFields.get(i).getKey()), null));
                }
            }
            Map<String, Object> groupResult = bizDemandGroup(simpleGroupList, conditions);
            // 转换方法1：基础转换
            List<DemandGroupNodeVO> treeNodes = bizDemandTransformToTree(groupResult, 0, conditions, bizDomainNameMap, productLineNameMap, deptNameMap, receiveManNameMap);

            // 排序（各层按 label 升序）
            sortTreeByLabel(treeNodes);
            return BaseResult.success(treeNodes);
        } else {
            for (ViewsGroupQueryList groupField : groupFields) {
                if (groupField.getType() == 1) {
                    parentCondition.setLabelCategoryIds(Collections.singletonList(Long.valueOf(groupField.getKey())));
                    groupField.setKey(ProductGroupFieldEnum.LABEL.getGroupField());
                }
            }
        }

        String selectField = buildBizSelectField(groupFields);
        String groupField = buildBizGroupField(groupFields);

        List<String> bizGroupFieldKeys = groupFields.stream().map(ViewsGroupQueryList::getKey).collect(Collectors.toList());

        BizDemandGroupQueryCondition groupCondition = BizDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .selectField(selectField)
                .groupField(groupField)
                .orderField(dynamicGroupQueryList.getOrderField())
                .build();

        List<BizDemandGroupFieldDO> rows = bizDemandComponent.groupTree(groupCondition);

        if (CollectionUtils.isEmpty(rows)) {
            return BaseResult.success(Collections.emptyList());
        }

        if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField())) {
            Set<Long> ids = extractUniqueIds(rows, BizDemandGroupFieldDO::getBizDomainId);
            if (CollectionUtils.isNotEmpty(ids)) {
                bizDomainNameMap = bizDomainMapper.getByIds(ids).stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
            }
        }
        if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField())) {
            Set<Long> productLineIds = extractUniqueIds(rows, BizDemandGroupFieldDO::getProductLineId);
            if (CollectionUtils.isNotEmpty(productLineIds)) {
                productLineNameMap = productLineMapper.getByIds(productLineIds).stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
            }
        }
        if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField())) {
            receiveManNameMap = getPersonMap(receiveManNameMap, rows.stream().map(BizDemandGroupFieldDO::getReceiveManId));
        }
        if (bizGroupFieldKeys.contains(ProductGroupFieldEnum.LABEL.getGroupField())) {
            labelNameMap = getLabelMap(labelNameMap, rows.stream().map(BizDemandGroupFieldDO::getLabelId));
        }
        if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.DEMAND_DEPT.getGroupField())) {
            Set<Long> deptIds = rows.stream().map(BizDemandGroupFieldDO::getDeptId).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(deptIds)) {
                Map<Long, GroupResponse> groupListTreeMap = bizDemandComponent.getGroupListTreeMap(deptIds);
                Map<String, String> map = new HashMap<>();
                Collection<GroupResponse> deptValues = groupListTreeMap.values();
                if (CollectionUtils.isNotEmpty(deptValues)) {
                    for (GroupResponse r : deptValues) {
                        map.put(r.getGroupId(), r.getGroupName());
                    }
                }
                deptNameMap = map;
            }
        }

        // 4) 构建树（一次性结果 → 树），并处理 type 维度的多值拆分
        List<DemandGroupNodeVO> roots = new ArrayList<>();
        // key: path
        Map<String, DemandGroupNodeVO> levelNodeMap = new HashMap<>();

        // 构建节点
        for (BizDemandGroupFieldDO row : rows) {
            buildBizNodesForRow(bizGroupFieldKeys, row, roots, levelNodeMap,
                    bizDomainNameMap, productLineNameMap, receiveManNameMap, labelNameMap, deptNameMap);
        }

        // 5) 计算合计：
        // 先为叶子节点赋值（叶子已经在构建时累加 total），然后从底向上汇总。
        computeTotalsBottomUp(roots);

        // 排序（各层按 label 升序）
        sortTreeByLabel(roots);

        return BaseResult.success(roots);
    }

    private Map<Long, String> getLabelMap(Map<Long, String> labelNameMap, Stream<Long> longStream) {
        List<Long> ids = longStream.filter(Objects::nonNull).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(ids)) {
            labelNameMap = labelMapper.getByIds(ids).stream().collect(Collectors.toMap(LabelDO::getId, LabelDO::getName, (a, b) -> b));
        }
        return labelNameMap;
    }

    private Map<String, String> getPersonMap(Map<String, String> receiveManNameMap, Stream<String> stringStream) {
        Set<String> receiveManIds = stringStream.filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(receiveManIds)) {
            List<BaseInfoResponse> responses = innerUserPersonClient.batchGetStaffInfos(receiveManIds, true);
            Map<String, String> map = new HashMap<>();
            if (CollectionUtils.isNotEmpty(responses)) {
                for (BaseInfoResponse r : responses) {
                    map.put(r.getAccount(), r.getAlias());
                }
            }
            receiveManNameMap = map;
        }
        return receiveManNameMap;
    }


    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> getBizDemandList(DynamicBizDemandGroupList dynamicGroupQueryList) {
        BizDemandQueryList bizDemandQueryList = dynamicGroupQueryList.getFilters();
        BizDemandGroupList parentBizDemandGroupCondition = dynamicGroupQueryList.getGroupFilters();
        List<ViewsGroupQueryList> groupFields = dynamicGroupQueryList.getGroupFields();
        // 转换查询条件
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);
        BizDemandGroupCondition parentCondition = BizDemandCopier.INSTANCE.convert(parentBizDemandGroupCondition);

        boolean resultIsEmpty = groupDuplicateUtil.isResultIsEmpty(bizDemandQueryList, condition);
        if (resultIsEmpty) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        // 标签
        if (CollectionUtils.isNotEmpty(bizDemandQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(bizDemandQueryList.getLabelCategoryIds())) {
            List<Long> labelIds = labelComponent.getLabelIds(bizDemandQueryList.getLabelIds(), bizDemandQueryList.getLabelCategoryIds());
            if (CollectionUtils.isEmpty(labelIds) && bizDemandQueryList.getContainLabel()) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setLabelIds(labelIds);
        }

        // 获取标签类别Id
        List<Long> labelCategoryIds = groupFields.stream().filter(field -> field.getType() == 1).map(field -> Long.valueOf(field.getKey())).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(labelCategoryIds)) {
            List<Long> bizDemandIds = new ArrayList<>();
            //查询等于标签id的需求
            List<Long> labelIds = parentCondition.getLabelIds();
            if (CollUtil.isNotEmpty(labelIds)) {
                List<Long> newLabelIds = labelComponent.getLabelIds(labelIds, labelCategoryIds);
                // 查询使用这些标签的需求id
                List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.BIZ_DEMAND.getCode());
                List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

                if (CollectionUtils.isEmpty(bizIds)) {
                    return BaseResult.success(ResultUtil.pageEmpty());
                }
                bizDemandIds.addAll(bizIds);
            }

            //查询不等于标签id的需求
            List<Long> notInLabelIds = parentCondition.getNotInLabelIds();
            if (CollUtil.isNotEmpty(notInLabelIds)) {
                List<Long> ids = labelMapper.getByCategoryIds(labelCategoryIds, false).stream().map(LabelDO::getId).collect(Collectors.toList());
                ids.removeAll(notInLabelIds);
                // 查询使用这些标签的需求id
                List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(ids, BizTypeEnum.BIZ_DEMAND.getCode());
                List<Long> notInBizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(notInBizIds)) {
                    bizDemandIds.addAll(notInBizIds);
                }
            }
            parentCondition.setContainIds(bizDemandIds);
        }

        BizDemandGroupQueryCondition groupCondition = BizDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .groupField(null)
                .orderField(dynamicGroupQueryList.getOrderField())
                .build();

        PageQueryResult<BizDemandVO> pageQueryResult = bizDemandComponent.groupList(groupCondition);

        if (bizDemandQueryList.getQuerySource() == 1) {
            // 交付项目来源查询需要特殊排序
            List<BizDemandVO> resultList = pageQueryResult.getResultList();
            groupDuplicateUtil.sortByCreateDate(resultList);
        }

        return BaseResult.success(pageQueryResult);
    }

    private void sortTreeByLabel(List<DemandGroupNodeVO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        // 处理其他
        String values = nodes.stream().filter(n -> !OTHER.equals(n.getLabel())).map(DemandGroupNodeVO::getFieldValue).collect(Collectors.joining(","));
        for (DemandGroupNodeVO node : nodes) {
            if (OTHER.equals(node.getLabel())) {
                node.setFieldValue(StringUtils.isNotBlank(values) ? values : "-1");
                node.setField(QUERY_OTHER_FIELD_MAP.get(node.getField()));
            }
        }
        nodes.sort(Comparator.comparing(DemandGroupNodeVO::getLabel, Comparator.nullsLast(String::compareTo)));
        for (DemandGroupNodeVO n : nodes) {
            sortTreeByLabel(n.getChildren());
        }
    }

    private void applyPrefixTotalsCorrection(List<DemandGroupNodeVO> roots,
                                             List<String> groupFields,
                                             List<Map<String, Long>> prefixTotals,
                                             int typeIndex) {
        // DFS 覆盖 type 之前层级的 total
        applyPrefixTotalsCorrectionRecursive(roots, groupFields, prefixTotals, typeIndex, 0, new ArrayList<>());
    }

    private void applyPrefixTotalsCorrectionRecursive(List<DemandGroupNodeVO> nodes,
                                                      List<String> groupFields,
                                                      List<Map<String, Long>> prefixTotals,
                                                      int typeIndex,
                                                      int level,
                                                      List<String> pathValues) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        boolean beforeType = typeIndex == -1 || level < typeIndex;
        for (DemandGroupNodeVO node : nodes) {
            List<String> newPath = new ArrayList<>(pathValues);
            newPath.add(node.getFieldValue());
            if (beforeType) {
                String prefixKey = buildPrefixKeyFromPath(groupFields, newPath, level);
                Long agg = prefixTotals.get(level).get(prefixKey);
                if (agg != null) {
                    node.setTotal(agg);
                }
            }
            if (level + 1 < groupFields.size() && CollectionUtils.isNotEmpty(node.getChildren())) {
                applyPrefixTotalsCorrectionRecursive(node.getChildren(), groupFields, prefixTotals, typeIndex, level + 1, newPath);
            }
        }
    }

    private void computeTotalsBottomUp(List<DemandGroupNodeVO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        for (DemandGroupNodeVO node : nodes) {
            if (CollectionUtils.isNotEmpty(node.getChildren())) {
                computeTotalsBottomUp(node.getChildren());
                long sum = node.getChildren().stream().map(DemandGroupNodeVO::getTotal).filter(Objects::nonNull).mapToLong(Long::longValue).sum();
                node.setTotal(sum);
            } else if (node.getTotal() == null) {
                node.setTotal(0L);
            }
        }
    }

    private void buildNodesForRow(List<String> groupFields,
                                  ProductDemandGroupFieldDO row,
                                  List<DemandGroupNodeVO> roots,
                                  Map<String, DemandGroupNodeVO> levelNodeMap,
                                  Map<Long, String> bizDomainNameMap,
                                  Map<Long, String> productLineNameMap,
                                  Map<String, String> ownerNameMap,
                                  Map<Long, String> labelNameMap) {
        // 取各层的值序列（遇到 type 多值拆分）
        List<List<String>> valuesPerLevel = new ArrayList<>();
        for (String gf : groupFields) {
            if (ProductGroupFieldEnum.TYPE.getGroupField().equals(gf)) {
                List<String> codes = parseTypeCodes(row.getType());
                if (CollectionUtils.isEmpty(codes)) {
                    valuesPerLevel.add(Collections.singletonList(""));
                } else {
                    valuesPerLevel.add(codes);
                }
            } else {
                String v = getValueFromRow(gf, row);
                valuesPerLevel.add(Collections.singletonList(v));
            }
        }

        // 递归笛卡尔展开
        addNodeRecursive(BizTypeEnum.PRODUCT_DEMAND.getCode(), 0, groupFields, valuesPerLevel, roots, levelNodeMap,
                bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap, null, row.getTotal());
    }

    private void buildBizNodesForRow(List<String> groupFields,
                                     BizDemandGroupFieldDO row,
                                     List<DemandGroupNodeVO> roots,
                                     Map<String, DemandGroupNodeVO> levelNodeMap,
                                     Map<Long, String> bizDomainNameMap,
                                     Map<Long, String> productLineNameMap,
                                     Map<String, String> receiveManNameMap,
                                     Map<Long, String> labelNameMap,
                                     Map<String, String> deptNameMap) {
        // 取各层的值序列
        List<List<String>> valuesPerLevel = new ArrayList<>();
        for (String gf : groupFields) {
            String v = getBizValueFromRow(gf, row);
            valuesPerLevel.add(Collections.singletonList(v));

        }

        // 递归笛卡尔展开
        addNodeRecursive(BizTypeEnum.BIZ_DEMAND.getCode(), 0, groupFields, valuesPerLevel, roots, levelNodeMap,
                bizDomainNameMap, productLineNameMap, receiveManNameMap, labelNameMap, deptNameMap, row.getTotal());
    }

    private void addNodeRecursive(Integer bizType,
                                  int level,
                                  List<String> groupFields,
                                  List<List<String>> valuesPerLevel,
                                  List<DemandGroupNodeVO> roots,
                                  Map<String, DemandGroupNodeVO> levelNodeMap,
                                  Map<Long, String> bizDomainNameMap,
                                  Map<Long, String> productLineNameMap,
                                  Map<String, String> ownerNameMap,
                                  Map<Long, String> labelNameMap,
                                  Map<String, String> deptNameMap,
                                  Long rowTotal) {
        if (level >= groupFields.size()) {
            return;
        }
        List<String> currentValues = valuesPerLevel.get(level);
        for (String value : currentValues) {
            String pathKey = buildPathKeyUpToLevel(groupFields, valuesPerLevel, level, value);
            DemandGroupNodeVO node = levelNodeMap.get(pathKey);
            if (node == null) {
                String gf = groupFields.get(level);
                String label = "";
                String field = "";
                if (BizTypeEnum.BIZ_DEMAND.getCode().equals(bizType)) {
                    label = buildBizLabelForValue(gf, value, bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap, deptNameMap);
                    field = BIZ_GROUP_FIELD_MAP.get(gf);
                } else if (BizTypeEnum.PRODUCT_DEMAND.getCode().equals(bizType)) {
                    label = buildProductLabelForValue(gf, value, bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap);
                    field = PRODUCT_GROUP_FIELD_MAP.get(gf);
                }
                node = new DemandGroupNodeVO();
                node.setField(field);
                node.setFieldValue(value);
                node.setLabel(label);
                node.setTotal(0L);
                node.setChildren(new ArrayList<>());
                levelNodeMap.put(pathKey, node);
                if (level == 0) {
                    roots.add(node);
                } else {
                    // 将本节点挂到父节点
                    DemandGroupNodeVO parent = levelNodeMap.get(buildPathKeyParent(groupFields, valuesPerLevel, level));
                    if (parent != null) {
                        parent.getChildren().add(node);
                    }
                }
            }

            if (level == groupFields.size() - 1) {
                node.setTotal(node.getTotal() + Optional.ofNullable(rowTotal).orElse(0L));
            } else {
                addNodeRecursive(bizType, level + 1, groupFields, valuesPerLevel, roots, levelNodeMap,
                        bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap, deptNameMap, rowTotal);
            }
        }
    }

    private String buildPathKeyParent(List<String> groupFields, List<List<String>> valuesPerLevel, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            String gf = groupFields.get(i);
            String v = valuesPerLevel.get(i).get(0);
            if (i > 0) sb.append('|');
            sb.append(gf).append('=').append(v);
        }
        return sb.toString();
    }

    private String buildPathKeyUpToLevel(List<String> groupFields, List<List<String>> valuesPerLevel, int level, String currentValue) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= level; i++) {
            String gf = groupFields.get(i);
            String v = (i == level) ? currentValue : valuesPerLevel.get(i).get(0);
            if (i > 0) sb.append('|');
            sb.append(gf).append('=').append(v);
        }
        return sb.toString();
    }

    private String buildPrefixKey(List<String> groupFields, ProductDemandGroupFieldDO row, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= level; i++) {
            String gf = groupFields.get(i);
            String v;
            if (ProductGroupFieldEnum.TYPE.getGroupField().equals(gf)) {
                v = String.valueOf(row.getType());
            } else {
                v = getValueFromRow(gf, row);
            }
            if (i > 0) sb.append('|');
            sb.append(gf).append('=').append(v);
        }
        return sb.toString();
    }

    private String buildPrefixKeyFromPath(List<String> groupFields, List<String> pathValues, int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= level; i++) {
            if (i > 0) sb.append('|');
            sb.append(groupFields.get(i)).append('=').append(pathValues.get(i));
        }
        return sb.toString();
    }

    private String getValueFromRow(String groupField, ProductDemandGroupFieldDO row) {
        if (ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            return row.getBizDomainId() == null ? "" : String.valueOf(row.getBizDomainId());
        }
        if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            return row.getProductLineId() == null ? "" : String.valueOf(row.getProductLineId());
        }
        if (ProductGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            return row.getStatus() == null ? "" : String.valueOf(row.getStatus());
        }
        if (ProductGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            return row.getPriority() == null ? "" : String.valueOf(row.getPriority());
        }
        if (ProductGroupFieldEnum.EXPECT_SCHEDULE_TIME.getGroupField().equals(groupField)) {
            return StringUtils.defaultString(row.getExpectScheduleTime(), "");
        }
        if (ProductGroupFieldEnum.OWNER.getGroupField().equals(groupField)) {
            return StringUtils.defaultString(row.getOwnerId(), "");
        }
        if (ProductGroupFieldEnum.TYPE.getGroupField().equals(groupField)) {
            return StringUtils.defaultString(row.getType(), "");
        }
        if (ProductGroupFieldEnum.LABEL.getGroupField().equals(groupField)) {
            return row.getLabelId() == null ? "" : String.valueOf(row.getLabelId());
        }
        return "";
    }

    private String getBizValueFromRow(String groupField, BizDemandGroupFieldDO row) {
        if (BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            return row.getBizDomainId() == null ? "" : String.valueOf(row.getBizDomainId());
        }
        if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            return row.getProductLineId() == null ? "" : String.valueOf(row.getProductLineId());
        }
        if (BizDemandGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            return row.getStatus() == null ? "" : String.valueOf(row.getStatus());
        }
        if (BizDemandGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            return row.getPriority() == null ? "" : String.valueOf(row.getPriority());
        }
        if (BizDemandGroupFieldEnum.DEMAND_DEPT.getGroupField().equals(groupField)) {
            return row.getDeptId() == null ? "" : String.valueOf(row.getDeptId());
        }
        if (BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField().equals(groupField)) {
            return StringUtils.defaultString(row.getReceiveManId(), "");
        }
        if (BizDemandGroupFieldEnum.TARGET_CUSTOMER.getGroupField().equals(groupField)) {
            return StringUtils.defaultString(row.getTargetCustomer(), "");
        }
        if (BizDemandGroupFieldEnum.LABEL.getGroupField().equals(groupField)) {
            return row.getLabelId() == null ? "" : String.valueOf(row.getLabelId());
        }
        return "";
    }

    private String buildProductLabelForValue(String groupField,
                                             String value,
                                             Map<Long, String> bizDomainNameMap,
                                             Map<Long, String> productLineNameMap,
                                             Map<String, String> ownerNameMap,
                                             Map<Long, String> labelNameMap) {
        if (ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return bizDomainNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return productLineNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (ProductGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return ProductDemandStatusEnum.getTextByCode(Integer.parseInt(value));
        }
        if (ProductGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return PriorityEnum.getTextByCode(Integer.parseInt(value));
        }
        if (ProductGroupFieldEnum.EXPECT_SCHEDULE_TIME.getGroupField().equals(groupField)) {
            return StringUtils.isBlank(value) ? OTHER : value;
        }
        if (ProductGroupFieldEnum.OWNER.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return ownerNameMap.getOrDefault(value, OTHER);
        }
        if (ProductGroupFieldEnum.TYPE.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return StringUtils.defaultIfBlank(ProductDemandTypeEnum.getTextByCode(Integer.parseInt(value)), OTHER);
        }
        if (ProductGroupFieldEnum.LABEL.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return labelNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        return OTHER;
    }

    private String buildBizLabelForValue(String groupField,
                                         String value,
                                         Map<Long, String> bizDomainNameMap,
                                         Map<Long, String> productLineNameMap,
                                         Map<String, String> ownerNameMap,
                                         Map<Long, String> labelNameMap,
                                         Map<String, String> deptNameMap) {
        if (BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return bizDomainNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return productLineNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (BizDemandGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return BizDemandStatusEnum.getTextByCode(Integer.parseInt(value));
        }
        if (BizDemandGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return PriorityEnum.getTextByCode(Integer.parseInt(value));
        }
        if (BizDemandGroupFieldEnum.TARGET_CUSTOMER.getGroupField().equals(groupField)) {
            return StringUtils.isBlank(value) ? OTHER : value;
        }
        if (BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return ownerNameMap.getOrDefault(value, OTHER);
        }
        if (BizDemandGroupFieldEnum.DEMAND_DEPT.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return deptNameMap.getOrDefault(value, OTHER);
        }
        if (BizDemandGroupFieldEnum.LABEL.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return labelNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        return OTHER;
    }

    private List<String> parseTypeCodes(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        String cleaned = raw.replaceAll("[\\[\\]]", "").trim();
        if (StringUtils.isBlank(cleaned)) {
            return Collections.emptyList();
        }
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }

    private String buildSelectField(List<ViewsGroupQueryList> groupFields) {
        return groupFields.stream()
                .map(ViewsGroupQueryList::getKey)
                .map(ProductSelectFieldEnum::getSourceFieldBySelectField)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));
    }

    private String buildGroupField(List<ViewsGroupQueryList> groupFields) {
        List<String> parts = new ArrayList<>();
        for (ViewsGroupQueryList gq : groupFields) {
            parts.add(ProductGroupFieldEnum.getSourceFieldByGroupField(gq.getKey()));
        }
        return String.join(",", parts);
    }

    private String buildBizSelectField(List<ViewsGroupQueryList> groupFields) {
        return groupFields.stream()
                .map(ViewsGroupQueryList::getKey)
                .map(BizDemandSelectFieldEnum::getSourceFieldBySelectField)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));
    }

    private String buildBizGroupField(List<ViewsGroupQueryList> groupFields) {
        List<String> parts = new ArrayList<>();
        for (ViewsGroupQueryList gq : groupFields) {
            parts.add(BizDemandGroupFieldEnum.getSourceFieldByGroupField(gq.getKey()));
        }
        return String.join(",", parts);
    }
}
