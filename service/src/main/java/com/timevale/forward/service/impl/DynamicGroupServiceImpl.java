package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductDemandGroupCondition;
import com.timevale.forward.dal.condition.ProductDemandGroupQueryCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.LabelCategoryMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.dal.entity.ProductDemandGroupFieldDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.DynamicGroupService;
import com.timevale.forward.facade.api.query.DynamicProductDemandGroupList;
import com.timevale.forward.facade.api.query.ProductDemandGroupList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.result.DemandGroupNodeVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProductLineAnalyseVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.GroupFieldEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProductDemandTypeEnum;
import com.timevale.forward.model.enums.SelectFieldEnum;
import com.timevale.forward.service.component.LabelComponent;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.duplicate.GroupDuplicateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private BizLabelMapper bizLabelMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private LabelCategoryMapper labelCategoryMapper;

    @Resource
    private ProductDemandComponent productDemandComponent;

    @Resource
    private GroupDuplicateUtil groupDuplicateUtil;

    private static final Map<String, String> GROUP_FIELD_MAP = new HashMap<>();
    
    private static final String OTHER = "其他";

    static {
        GROUP_FIELD_MAP.put(GroupFieldEnum.BIZ_DOMAIN.getGroupField(), "bizDomainId");
        GROUP_FIELD_MAP.put(GroupFieldEnum.PRODUCT_LINE.getGroupField(), "productLineId");
        GROUP_FIELD_MAP.put(GroupFieldEnum.LABEL_CATEGORY.getGroupField(), "labelCategoryId");
        GROUP_FIELD_MAP.put(GroupFieldEnum.TYPE.getGroupField(), "type");
        GROUP_FIELD_MAP.put(GroupFieldEnum.STATUS.getGroupField(), "status");
        GROUP_FIELD_MAP.put(GroupFieldEnum.PRIORITY.getGroupField(), "priority");
        GROUP_FIELD_MAP.put(GroupFieldEnum.EXPECT_SCHEDULE_TIME.getGroupField(), "expectScheduleTime");
        GROUP_FIELD_MAP.put(GroupFieldEnum.OWNER.getGroupField(), "ownerId");
    }

    @Override
    public BaseResult<List<DemandGroupNodeVO>> getProductDemandsGroupTree(DynamicProductDemandGroupList DynamicProductDemandGroupList) {
        List<String> groupFields = DynamicProductDemandGroupList.getGroupFields();
        if (CollUtil.isEmpty(groupFields)) {
            throw new BaseBizRuntimeException("分组字段不能为空");
        }

        // 1) 构建基础查询条件（复用一层分组的参数处理逻辑）
        ProductDemandQueryList productDemandQueryList = DynamicProductDemandGroupList.getFilters();
        ProductDemandGroupList parentProductDemandQueryList = DynamicProductDemandGroupList.getParentConditions();
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        ProductDemandGroupCondition parentCondition = ProductDemandCopier.INSTANCE.convert(parentProductDemandQueryList);

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

        // 2) 组织一次性聚合 SQL 的 selectField 与 groupField
        String selectField = buildSelectField(groupFields);
        String groupField = buildGroupField(groupFields);

        ProductDemandGroupQueryCondition groupCondition = ProductDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .selectField(selectField)
                .groupField(groupField)
                .orderField(DynamicProductDemandGroupList.getOrderField())
                .build();

        List<ProductDemandGroupFieldDO> rows = productDemandComponent.getGroupTree(groupCondition);
        if (CollectionUtils.isEmpty(rows)) {
            return BaseResult.success(Collections.emptyList());
        }

        // 3) 名称字典一次性查询
        Map<Long, String> bizDomainNameMap = Collections.emptyMap();
        Map<Long, String> productLineNameMap = Collections.emptyMap();
        Map<String, String> ownerNameMap = Collections.emptyMap();
        Map<Long, String> labelNameMap = Collections.emptyMap();

        if (groupFields.contains(GroupFieldEnum.BIZ_DOMAIN.getGroupField())) {
            Set<Long> ids = extractUniqueIds(rows, ProductDemandGroupFieldDO::getBizDomainId);
            if (CollectionUtils.isNotEmpty(ids)) {
                bizDomainNameMap = bizDomainMapper.getByIds(ids).stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
            }
        }
        if (groupFields.contains(GroupFieldEnum.PRODUCT_LINE.getGroupField())) {
            Set<Long> productLineIds = extractUniqueIds(rows, ProductDemandGroupFieldDO::getProductLineId);
            if (CollectionUtils.isNotEmpty(productLineIds)) {
                productLineNameMap = productLineMapper.getByIds(productLineIds).stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
            }
        }
        if (groupFields.contains(GroupFieldEnum.OWNER.getGroupField())) {
            Set<String> ownerIds = rows.stream().map(ProductDemandGroupFieldDO::getOwnerId).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            if (CollectionUtils.isNotEmpty(ownerIds)) {
                List<BaseInfoResponse> responses = innerUserPersonClient.batchGetStaffInfos(ownerIds, true);
                Map<String, String> map = new HashMap<>();
                if (CollectionUtils.isNotEmpty(responses)) {
                    for (BaseInfoResponse r : responses) {
                        map.put(r.getAccount(), r.getAlias());
                    }
                }
                ownerNameMap = map;
            }
        }
        if (groupFields.contains(GroupFieldEnum.LABEL_CATEGORY.getGroupField())) {
            List<Long> ids = rows.stream().map(ProductDemandGroupFieldDO::getLabelCategoryId).filter(Objects::nonNull).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(ids)) {
                labelNameMap = labelCategoryMapper.get(ids).stream().collect(Collectors.toMap(LabelCategoryDO::getId, LabelCategoryDO::getName, (a, b) -> b));
            }
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
            for (int level = 0; level < groupFields.size(); level++) {
                String prefixKey = buildPrefixKey(groupFields, row, level);
                prefixTotals.get(level).merge(prefixKey, Optional.ofNullable(row.getTotal()).orElse(0L), Long::sum);
            }
        }

        int typeIndex = groupFields.indexOf(GroupFieldEnum.TYPE.getGroupField());

        // 构建节点（对 type 维度进行拆分）
        for (ProductDemandGroupFieldDO row : rows) {
            buildNodesForRow(groupFields, row, roots, levelNodeMap,
                    bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap);
        }

        // 5) 计算合计：
        // 先为叶子节点赋值（叶子已经在构建时累加 total），然后从底向上汇总。
        computeTotalsBottomUp(roots);

        // 若包含 type 维度，则对 type 上层的节点用 prefixTotals 进行去重修正（避免被子层的多值重复计数放大）
        if (typeIndex >= 0) {
            applyPrefixTotalsCorrection(roots, groupFields, prefixTotals, typeIndex);
        }

        // 排序（各层按 label 升序）
        sortTreeByLabel(roots);

        return BaseResult.success(roots);
    }

    // 提取去重 ID 集合的通用方法
    private Set<Long> extractUniqueIds(List<ProductDemandGroupFieldDO> rows,
                                       Function<ProductDemandGroupFieldDO, Long> mapper) {
        return rows.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public BaseResult<QueryResultVO<ProductDemandVO>> getProductDemandList(DynamicProductDemandGroupList DynamicProductDemandGroupList) {
        if (Objects.isNull(DynamicProductDemandGroupList.getParentConditions())) {
            throw new BaseBizRuntimeException("父分组查询条件不能为空");
        }

        ProductDemandQueryList productDemandQueryList = DynamicProductDemandGroupList.getFilters();
        ProductDemandGroupList parentProductDemandQueryList = DynamicProductDemandGroupList.getParentConditions();
        log.info("产品需求接收参数:{}", DynamicProductDemandGroupList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        ProductDemandGroupCondition parentCondition = ProductDemandCopier.INSTANCE.convert(parentProductDemandQueryList);

        if (groupDuplicateUtil.setOwnerIdByAscription(productDemandQueryList, userInfo, condition, innerUserPersonClient)) {
            return BaseResult.success(ResultUtil.queryResultEmpty());
        }

        //是否打标
        if (CollectionUtils.isNotEmpty(productDemandQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(productDemandQueryList.getLabelCategoryIds())) {
            Boolean containLabel = productDemandQueryList.getContainLabel();

            List<Long> newLabelIds = labelComponent.getLabelIds(productDemandQueryList.getLabelIds(), productDemandQueryList.getLabelCategoryIds());

            // 查询包含且类别下没有标签
            if (CollectionUtils.isEmpty(newLabelIds) && Boolean.TRUE.equals(containLabel)) {
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }

            // 查询使用这些标签的需求id
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (Boolean.TRUE.equals(containLabel)) {
                if (CollectionUtils.isEmpty(bizIds)) {
                    return BaseResult.success(ResultUtil.queryResultEmpty());
                }
                condition.setInProductDemandIds(bizIds);
            } else {
                condition.setNotInProductDemandIds(bizIds);
            }

        }

        // 完整查询
        List<ProductDemandListDO> allProductDemandListDO = productDemandComponent.list(ProductDemandCopier.INSTANCE.convert(condition));
        List<ProductLineAnalyseVO> analyseVOList = groupDuplicateUtil.getProductLineAnalyseVOS(allProductDemandListDO);

        List<Long> conditionSubProductLineIdList = productDemandQueryList.getSubProductLineIds();
        if (CollectionUtils.isNotEmpty(conditionSubProductLineIdList)) {
            Set<Long> resultProductLineIdSet = analyseVOList.stream().map(ProductLineAnalyseVO::getProductLineId).collect(Collectors.toSet());
            List<Long> queryProductLineIdList = conditionSubProductLineIdList.stream().filter(resultProductLineIdSet::contains).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(queryProductLineIdList)) {
                return BaseResult.success(ResultUtil.queryResultEmpty());
            } else {
                condition.setProductLineIds(queryProductLineIdList);
            }
        }

        //是否打标
        Long labelCategoryId = parentCondition.getLabelCategoryId();
        if (Objects.nonNull(labelCategoryId)) {
            List<Long> newLabelIds = labelComponent.getLabelIds(null, productDemandQueryList.getLabelCategoryIds());

            // 查询使用这些标签的需求id
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(bizIds)) {
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }
            parentCondition.setInProductDemandIds(bizIds);
        }

        ProductDemandGroupQueryCondition groupCondition = ProductDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .groupField(null)
                .orderField(DynamicProductDemandGroupList.getOrderField())
                .build();

        List<ProductDemandListDO> productDemandListDO = productDemandComponent.getGroupList(groupCondition);

        List<ProductDemandVO> productDemandVOList = ProductDemandCopier.INSTANCE.convert(productDemandListDO);

        if (CollectionUtils.isEmpty(productDemandVOList)) {
            return BaseResult.success(ResultUtil.queryResultEmpty());
        }
        QueryResultVO<ProductDemandVO> queryResultVO = groupDuplicateUtil.getDemandVOQueryResultVO(productDemandListDO, productDemandVOList, analyseVOList);

        return BaseResult.success(queryResultVO);
    }

    private void sortTreeByLabel(List<DemandGroupNodeVO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        // 处理其他
        String values = nodes.stream().filter(n -> !OTHER.equals(n.getLabel())).map(DemandGroupNodeVO::getFieldValue).collect(Collectors.joining(","));
        for (DemandGroupNodeVO node : nodes) {
            if (OTHER.equals(node.getLabel())) {
                node.setFieldValue(values);
                node.setField(node.getField() + "s");
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
            if (GroupFieldEnum.TYPE.getGroupField().equals(gf)) {
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
        addNodeRecursive(0, groupFields, valuesPerLevel, roots, levelNodeMap,
                bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap, row.getTotal());
    }

    private void addNodeRecursive(int level,
                                  List<String> groupFields,
                                  List<List<String>> valuesPerLevel,
                                  List<DemandGroupNodeVO> roots,
                                  Map<String, DemandGroupNodeVO> levelNodeMap,
                                  Map<Long, String> bizDomainNameMap,
                                  Map<Long, String> productLineNameMap,
                                  Map<String, String> ownerNameMap,
                                  Map<Long, String> labelNameMap,
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
                String label = buildLabelForValue(gf, value, bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap);
                node = DemandGroupNodeVO.builder()
                        .field(GROUP_FIELD_MAP.get(gf))
                        .fieldValue(value)
                        .label(label)
                        .total(0L)
                        .children(new ArrayList<>())
                        .build();
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
                addNodeRecursive(level + 1, groupFields, valuesPerLevel, roots, levelNodeMap,
                        bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap, rowTotal);
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
            if (GroupFieldEnum.TYPE.getGroupField().equals(gf)) {
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
        if (GroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            return row.getBizDomainId() == null ? "" : String.valueOf(row.getBizDomainId());
        }
        if (GroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            return row.getProductLineId() == null ? "" : String.valueOf(row.getProductLineId());
        }
        if (GroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            return row.getStatus() == null ? "" : String.valueOf(row.getStatus());
        }
        if (GroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            return row.getPriority() == null ? "" : String.valueOf(row.getPriority());
        }
        if (GroupFieldEnum.EXPECT_SCHEDULE_TIME.getGroupField().equals(groupField)) {
            return StringUtils.defaultString(row.getExpectScheduleTime(), "");
        }
        if (GroupFieldEnum.OWNER.getGroupField().equals(groupField)) {
            return StringUtils.defaultString(row.getOwnerId(), "");
        }
        if (GroupFieldEnum.TYPE.getGroupField().equals(groupField)) {
            return StringUtils.defaultString(row.getType(), "");
        }
        if (GroupFieldEnum.LABEL_CATEGORY.getGroupField().equals(groupField)) {
            return row.getLabelCategoryId() == null ? "" : String.valueOf(row.getLabelCategoryId());
        }
        return "";
    }

    private String buildLabelForValue(String groupField,
                                      String value,
                                      Map<Long, String> bizDomainNameMap,
                                      Map<Long, String> productLineNameMap,
                                      Map<String, String> ownerNameMap,
                                      Map<Long, String> labelNameMap) {
        if (GroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return bizDomainNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (GroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return productLineNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (GroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return ProductDemandStatusEnum.getTextByCode(Integer.parseInt(value));
        }
        if (GroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return PriorityEnum.getTextByCode(Integer.parseInt(value));
        }
        if (GroupFieldEnum.EXPECT_SCHEDULE_TIME.getGroupField().equals(groupField)) {
            return StringUtils.isBlank(value) ? OTHER : value;
        }
        if (GroupFieldEnum.OWNER.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return ownerNameMap.getOrDefault(value, OTHER);
        }
        if (GroupFieldEnum.TYPE.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return StringUtils.defaultIfBlank(ProductDemandTypeEnum.getTextByCode(Integer.parseInt(value)), OTHER);
        }
        if (GroupFieldEnum.LABEL_CATEGORY.getGroupField().equals(groupField)) {
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

    private String buildSelectField(List<String> groupFields) {
        return groupFields.stream()
                .map(SelectFieldEnum::getSourceFieldBySelectField)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));
    }

    private String buildGroupField(List<String> groupFields) {
        List<String> parts = new ArrayList<>();
        for (String gf : groupFields) {
            parts.add(GroupFieldEnum.getSourceFieldByGroupField(gf));
        }
        return String.join(",", parts);
    }
}
