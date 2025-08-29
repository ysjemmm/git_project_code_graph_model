package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.github.pagehelper.PageHelper;
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
import com.timevale.forward.dal.entity.ProjectProductLineBizDomain;
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
import com.timevale.forward.service.constant.CommonConstant;
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
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "0", "labelId");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "1", "labelId1");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "2", "labelId2");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "3", "labelId3");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "4", "labelId4");
        PRODUCT_GROUP_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "5", "labelId5");
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
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.LABEL.getGroupField() + "0", "labelId");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.LABEL.getGroupField() + "1", "labelId1");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.LABEL.getGroupField() + "2", "labelId2");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.LABEL.getGroupField() + "3", "labelId3");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.LABEL.getGroupField() + "4", "labelId4");
        BIZ_GROUP_FIELD_MAP.put(BizDemandGroupFieldEnum.LABEL.getGroupField() + "5", "labelId5");
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
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "0", ProductDemandGroupFieldDO::getLabelId);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "1", ProductDemandGroupFieldDO::getLabelId1);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "2", ProductDemandGroupFieldDO::getLabelId2);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "3", ProductDemandGroupFieldDO::getLabelId3);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "4", ProductDemandGroupFieldDO::getLabelId4);
        FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "5", ProductDemandGroupFieldDO::getLabelId5);
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
        BIZ_FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "0", BizDemandGroupFieldDO::getLabelId);
        BIZ_FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "1", BizDemandGroupFieldDO::getLabelId1);
        BIZ_FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "2", BizDemandGroupFieldDO::getLabelId2);
        BIZ_FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "3", BizDemandGroupFieldDO::getLabelId3);
        BIZ_FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "4", BizDemandGroupFieldDO::getLabelId4);
        BIZ_FUNCTION_FIELD_MAP.put(ProductGroupFieldEnum.LABEL.getGroupField() + "5", BizDemandGroupFieldDO::getLabelId5);
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
        Map<Long, String> bizDomainNameMap;
        Map<Long, String> productLineNameMap;
        Map<String, String> ownerNameMap = Collections.emptyMap();
        Map<Long, String> labelNameMap = Collections.emptyMap();

        long count = groupFields.stream().filter(groupField -> groupField.getType() == 1).count();
        List<Long> labelCategoryId = groupFields.stream()
                .filter(groupField -> groupField.getType() == 1)
                .map(groupField -> Long.valueOf(groupField.getKey()))
                .collect(Collectors.toList());

        Map<Long, Map<Long, String>> labelMaps = new HashMap<>(labelCategoryId.size());
        if (CollUtil.isNotEmpty(labelCategoryId)) {
            // 定义分组条件
            List<LabelDO> byCategoryIds = labelMapper.getByCategoryIds(labelCategoryId, false);
            // 根据不同的标签类别，得到以分组id为key,不同的标签Map集合的Map集合，key为标签id，value为标签名称
            labelMaps = byCategoryIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(LabelDO::getLabelCategoryId,
                            Collectors.toMap(LabelDO::getId, LabelDO::getName, (a, b) -> b)));
        }

        List<String> groupFieldKeys = groupFields.stream().map(ViewsGroupQueryList::getKey).collect(Collectors.toList());

        List<Long> bizDomainIds = condition.getBizDomainIds();
        if (CollectionUtils.isNotEmpty(bizDomainIds)) {
            bizDomainNameMap = bizDomainMapper.getByIds(bizDomainIds).stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
        } else {
            bizDomainNameMap = bizDomainMapper.selectAllBizDomain().stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
        }

        List<Long> productLineIds = condition.getProductLineIds();
        if (CollectionUtils.isNotEmpty(productLineIds)) {
            productLineNameMap = productLineMapper.getByIds(productLineIds).stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
        } else {
            productLineNameMap = productLineMapper.selectAllProductLine().stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
        }

        List<Long> bizDomainIdList = new ArrayList<>(bizDomainNameMap.keySet());
        List<Long> productLineIdList = new ArrayList<>(productLineNameMap.keySet());
        List<ProjectProductLineBizDomain> plineAndBizDomain = productLineMapper.getPlineAndBizDomainList(bizDomainIdList, productLineIdList);
        Map<Long, Map<Long, String>> bizDomainMap = plineAndBizDomain
                .stream()
                .collect(Collectors.groupingBy(
                        ProjectProductLineBizDomain::getBizDomainId,
                        Collectors.toMap(
                                ProjectProductLineBizDomain::getProductLineId,
                                ProjectProductLineBizDomain::getProductLineName,
                                // 处理重复键的情况
                                (existing, replacement) -> existing
                        )
                ));
        Map<Long, Map<Long, String>> lineMap = plineAndBizDomain
                .stream()
                .collect(Collectors.groupingBy(
                        ProjectProductLineBizDomain::getProductLineId,
                        Collectors.toMap(
                                ProjectProductLineBizDomain::getBizDomainId,
                                ProjectProductLineBizDomain::getBizDomainName,
                                (existing, replacement) -> existing
                        )
                ));

        Map<String, Map<String, String>> enumMap = getEnumMap(condition);

        if (count > 1) {
            parentCondition.setLabelCategoryIds(labelCategoryId);
            ProductDemandGroupQueryCondition groupCondition = ProductDemandGroupQueryCondition.builder()
                    .condition(condition)
                    .parentCondition(parentCondition)
                    .build();
            List<ProductDemandGroupFieldDO> simpleGroupList = productDemandComponent.getSimpleGroupList(groupCondition);

            // 查询不在标签类别的需求数
            parentCondition.setLabelCategoryIds(null);
            parentCondition.setNotInLabelCategoryIds(labelCategoryId);
            ProductDemandGroupQueryCondition groupCountCondition = ProductDemandGroupQueryCondition.builder()
                    .condition(condition)
                    .parentCondition(parentCondition)
                    .build();
            Long simpleGroupCount = productDemandComponent.getSimpleGroupCount(groupCountCondition);
            if (CollectionUtils.isEmpty(simpleGroupList)) {
                simpleGroupList.add(new ProductDemandGroupFieldDO());
            }

            if (groupFieldKeys.contains(ProductGroupFieldEnum.OWNER.getGroupField())) {
                ownerNameMap = getPersonMap(ownerNameMap, simpleGroupList.stream().map(ProductDemandGroupFieldDO::getOwnerId));
            }

            if (groupFields.stream().anyMatch(e -> e.getType() == 1)) {
                labelNameMap = getLabelMap(labelNameMap, simpleGroupList.stream().map(ProductDemandGroupFieldDO::getLabelId));
            }

            List<ProductGroupCondition> conditions = new ArrayList<>(5);

            // 记录标签字段的索引
            int labelFieldIndex = 1;

            for (ViewsGroupQueryList groupField : groupFields) {
                if (groupField.getType() == 1) {
                    // 标签类别层：使用对应的 label1-label5 字段提取函数
                    String fieldName = ProductGroupFieldEnum.LABEL.getGroupField() + labelFieldIndex;
                    Function<ProductDemandGroupFieldDO, Object> labelExtractor = FUNCTION_FIELD_MAP.get(fieldName);
                    conditions.add(new ProductGroupCondition(
                            fieldName,
                            labelExtractor
                    ));
                    labelFieldIndex++;
                } else {
                    conditions.add(new ProductGroupCondition(groupField.getKey(), FUNCTION_FIELD_MAP.get(groupField.getKey())));
                }
            }
            // 若包含 type 维度，则进行多值拆分
            List<ProductDemandGroupFieldDO> dataForGrouping = groupFieldKeys.contains(ProductGroupFieldEnum.TYPE.getGroupField())
                    ? expandTypeMultiValues(simpleGroupList)
                    : simpleGroupList;

            // 根据分组条件重新组装数据，填充 label1-label5 字段
            dataForGrouping = populateLabelFields(dataForGrouping, groupFields, labelMaps);

            Map<String, Object> groupResult = group(dataForGrouping, conditions);

            // 从Map<Long, Map<Long, String>>中提取所有的值，构建成Map<Long, String>集合
            bizDomainNameMap = lineMap.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(innerMap -> innerMap.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            // 处理key冲突，保留第一个值
                            (v1, v2) -> v1
                    ));

            // 从Map<Long, Map<Long, String>>中提取所有的值，构建成Map<Long, String>集合
            productLineNameMap = bizDomainMap.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(innerMap -> innerMap.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            // 处理key冲突，保留第一个值
                            (v1, v2) -> v1
                    ));
            // 转换方法
            List<DemandGroupNodeVO> treeNodes = transformToTree(enumMap, groupResult, 0, conditions, bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap, bizDomainMap, lineMap);

            // 排序（各层按 label 升序）
            sortTreeByLabel(treeNodes);

            addNotInLabelCategoryDemandGroupNode(labelMaps, simpleGroupCount, treeNodes);
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
        // 重新赋值
        groupFieldKeys = groupFields.stream().map(ViewsGroupQueryList::getKey).collect(Collectors.toList());

        ProductDemandGroupQueryCondition groupCondition = ProductDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .selectField(selectField)
                .groupField(groupField)
                .build();

        List<ProductDemandGroupFieldDO> rows = productDemandComponent.getGroupTree(groupCondition);

        Long simpleGroupCount = 0L;
        if (count > 0) {
            // 查询不在标签类别的需求数
            parentCondition.setNotInLabelCategoryIds(parentCondition.getLabelCategoryIds());
            parentCondition.setLabelCategoryIds(null);
            ProductDemandGroupQueryCondition groupCountCondition = ProductDemandGroupQueryCondition.builder()
                    .condition(condition)
                    .parentCondition(parentCondition)
                    .build();
            simpleGroupCount = productDemandComponent.getSimpleGroupCount(groupCountCondition);
        }
        if (CollectionUtils.isEmpty(rows)) {
            rows.add(new ProductDemandGroupFieldDO());
        }

        if (groupFieldKeys.contains(ProductGroupFieldEnum.OWNER.getGroupField())) {
            ownerNameMap = getPersonMap(ownerNameMap, rows.stream().map(ProductDemandGroupFieldDO::getOwnerId));
        }
        if (groupFields.stream().anyMatch(e -> e.getType() == 1)) {
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

        // 在节点构建完成后，对根节点进行完整填充
        fillMissingGroupNodes(roots, groupFieldKeys, bizDomainMap, lineMap, enumMap);

        // 若包含 type 维度，则对 type 上层的节点用 prefixTotals 进行去重修正（避免被子层的多值重复计数放大）
        if (typeIndex >= 0) {
            applyPrefixTotalsCorrection(roots, groupFieldKeys, prefixTotals, typeIndex);
        }

        // 排序（各层按 label 升序）
        sortTreeByLabel(roots);
        if (count > 0) {
            addNotInLabelCategoryDemandGroupNode(labelMaps, simpleGroupCount, roots);
        }
        return BaseResult.success(roots);
    }

    private Map<String, Map<String, String>> getEnumMap(ProductDemandListCondition condition) {
        Map<String, Map<String, String>> enumMap = new HashMap<>(3);
        Map<String, String> priorityMap = new HashMap<>();
        Map<String, String> statusMap = new HashMap<>();
        Map<String, String> typeMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(condition.getPriorities())) {
            for (Integer priority : condition.getPriorities()) {
                if (priority != null) {
                    String priorityText = PriorityEnum.getTextByCode(priority);
                    if (StringUtils.isNotBlank(priorityText)) {
                        priorityMap.put(String.valueOf(priority), priorityText);
                    }
                }
            }
        } else {
            for (PriorityEnum priorityEnum : PriorityEnum.values()) {
                priorityMap.put(String.valueOf(priorityEnum.getCode()), priorityEnum.getText());
            }
        }

        if (CollectionUtils.isNotEmpty(condition.getStatus())) {
            for (Integer status : condition.getStatus()) {
                if (status != null) {
                    String statusText = ProductDemandStatusEnum.getTextByCode(status);
                    if (StringUtils.isNotBlank(statusText)) {
                        statusMap.put(String.valueOf(status), statusText);
                    }
                }
            }
        } else {
            for (ProductDemandStatusEnum statusEnum : ProductDemandStatusEnum.values()) {
                statusMap.put(String.valueOf(statusEnum.getCode()), statusEnum.getText());
            }
        }

        if (StringUtils.isNotEmpty(condition.getTypes())) {
            List<String> types = Arrays.asList(condition.getTypes());
            for (String type : types) {
                if (StringUtils.isNotEmpty(type)) {
                    String typeText = ProductDemandTypeEnum.getTextByCode(Integer.valueOf(type));
                    if (StringUtils.isNotBlank(typeText)) {
                        typeMap.put(type, typeText);
                    }
                }
            }
        } else {
            for (ProductDemandTypeEnum typeEnum : ProductDemandTypeEnum.values()) {
                typeMap.put(String.valueOf(typeEnum.getCode()), typeEnum.getText());
            }
        }

        enumMap.put(ProductGroupFieldEnum.STATUS.getGroupField(), statusMap);
        enumMap.put(ProductGroupFieldEnum.PRIORITY.getGroupField(), priorityMap);
        enumMap.put(ProductGroupFieldEnum.TYPE.getGroupField(), typeMap);

        return enumMap;
    }

    private Map<String, Map<String, String>> getBizEnumMap(BizDemandListCondition condition) {
        Map<String, Map<String, String>> enumMap = new HashMap<>(2);
        Map<String, String> priorityMap = new HashMap<>();
        Map<String, String> statusMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(condition.getPriorityList())) {
            for (Integer priority : condition.getPriorityList()) {
                if (priority != null) {
                    String priorityText = PriorityEnum.getTextByCode(priority);
                    if (StringUtils.isNotBlank(priorityText)) {
                        priorityMap.put(String.valueOf(priority), priorityText);
                    }
                }
            }
        } else {
            for (PriorityEnum priorityEnum : PriorityEnum.values()) {
                priorityMap.put(String.valueOf(priorityEnum.getCode()), priorityEnum.getText());
            }
        }

        if (CollectionUtils.isNotEmpty(condition.getStatusList())) {
            for (Integer status : condition.getStatusList()) {
                if (status != null) {
                    String statusText = BizDemandStatusEnum.getTextByCode(status);
                    if (StringUtils.isNotBlank(statusText)) {
                        statusMap.put(String.valueOf(status), statusText);
                    }
                }
            }
        } else {
            for (BizDemandStatusEnum statusEnum : BizDemandStatusEnum.values()) {
                statusMap.put(String.valueOf(statusEnum.getCode()), statusEnum.getText());
            }
        }

        enumMap.put(BizDemandGroupFieldEnum.STATUS.getGroupField(), statusMap);
        enumMap.put(BizDemandGroupFieldEnum.PRIORITY.getGroupField(), priorityMap);

        return enumMap;
    }

    // 在这个位置实现填充所有分组业务域和产品线的方法
    private void fillAllGroupNodes(Map<String, Object> mapResult,
                                   List<DemandGroupNodeVO> nodes,
                                   int currentLevel,
                                   List<ProductGroupCondition> conditions,
                                   Map<Long, String> bizDomainNameMap,
                                   Map<Long, String> productLineNameMap,
                                   Map<String, Map<String, String>> enumMap,
                                   Map<Long, Map<Long, String>> bizDomainMap,
                                   Map<Long, Map<Long, String>> lineMap) {
        // 如果当前层级是业务域分组
        ProductGroupCondition groupCondition = conditions.get(currentLevel);
        String field = groupCondition.getFieldName();

        if (ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(field)) {
            // 遍历所有业务域，确保每个都出现在结果中
            for (Map.Entry<Long, String> entry : bizDomainNameMap.entrySet()) {
                String bizDomainIdStr = String.valueOf(entry.getKey());
                String bizDomainName = entry.getValue();

                // 检查该业务域是否已存在于结果中
                if (!mapResult.containsKey(bizDomainIdStr)) {
                    // 如果不存在，创建一个空的节点，total为0
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(bizDomainIdStr);
                    node.setLabel(bizDomainName);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = transformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                bizDomainNameMap, bizDomainMap.get(entry.getKey()), null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    // 如果存在，确保其子节点也完整
                    ensureChildrenComplete(nodes, bizDomainIdStr, currentLevel, conditions,
                            bizDomainNameMap, bizDomainMap.get(entry.getKey()), enumMap, bizDomainMap, lineMap);
                }
            }
        } else if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(field)) {
            // 遍历所有产品线，确保每个都出现在结果中
            for (Map.Entry<Long, String> entry : productLineNameMap.entrySet()) {
                String productLineIdStr = String.valueOf(entry.getKey());
                String productLineName = entry.getValue();

                // 检查该产品线是否已存在于结果中
                if (!mapResult.containsKey(productLineIdStr)) {
                    // 如果不存在，创建一个空的节点，total为0
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(productLineIdStr);
                    node.setLabel(productLineName);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = transformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                lineMap.get(entry.getKey()), productLineNameMap, null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    // 如果存在，确保其子节点也完整
                    ensureChildrenComplete(nodes, productLineIdStr, currentLevel, conditions,
                            lineMap.get(entry.getKey()), productLineNameMap, enumMap, bizDomainMap, lineMap);
                }
            }
        } else if (ProductGroupFieldEnum.PRIORITY.getGroupField().equals(field)) {
            // 遍历所有优先级，确保每个都出现在结果中
            enumMap.get(ProductGroupFieldEnum.PRIORITY.getGroupField()).forEach((priority, priorityName) -> {
                if (!mapResult.containsKey(priority)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(priority);
                    node.setLabel(priorityName);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = transformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                bizDomainNameMap, productLineNameMap, null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    ensureChildrenComplete(nodes, priority, currentLevel, conditions,
                            bizDomainNameMap, productLineNameMap, enumMap, bizDomainMap, lineMap);
                }
            });
        } else if (ProductGroupFieldEnum.TYPE.getGroupField().equals(field)) {
            // 遍历所有类型，确保每个都出现在结果中
            enumMap.get(ProductGroupFieldEnum.TYPE.getGroupField()).forEach((type, typeName) -> {
                if (!mapResult.containsKey(type)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(type);
                    node.setLabel(typeName);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = transformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                bizDomainNameMap, productLineNameMap, null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    ensureChildrenComplete(nodes, type, currentLevel, conditions,
                            bizDomainNameMap, productLineNameMap, enumMap, bizDomainMap, lineMap);
                }
            });
        } else if (ProductGroupFieldEnum.STATUS.getGroupField().equals(field)) {
            // 遍历所有状态，确保每个都出现在结果中
            enumMap.get(ProductGroupFieldEnum.STATUS.getGroupField()).forEach((status, statusName) -> {
                if (!mapResult.containsKey(status)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(status);
                    node.setLabel(status);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = transformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                bizDomainNameMap, productLineNameMap, null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    ensureChildrenComplete(nodes, status, currentLevel, conditions,
                            bizDomainNameMap, productLineNameMap, enumMap, bizDomainMap, lineMap);
                }
            });
        }
    }

    // 确保现有节点的子节点完整
    private void ensureChildrenComplete(List<DemandGroupNodeVO> nodes, String fieldValue, int currentLevel,
                                        List<ProductGroupCondition> conditions,
                                        Map<Long, String> bizDomainNameMap,
                                        Map<Long, String> productLineNameMap,
                                        Map<String, Map<String, String>> enumMap,
                                        Map<Long, Map<Long, String>> bizDomainMap,
                                        Map<Long, Map<Long, String>> lineMap) {
        for (DemandGroupNodeVO node : nodes) {
            if (fieldValue.equals(node.getFieldValue()) &&
                    (node.getChildren() == null || node.getChildren().isEmpty()) &&
                    currentLevel + 1 < conditions.size()) {

                Map<String, Object> emptyChildMap = new HashMap<>();
                List<DemandGroupNodeVO> childNodes = transformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                        bizDomainNameMap, productLineNameMap, null, null, bizDomainMap, lineMap);
                node.setChildren(childNodes);
            }
        }
    }

    // 在这个位置实现填充所有分组业务域和产品线的方法
    private void fillBizAllGroupNodes(Map<String, Object> mapResult,
                                      List<DemandGroupNodeVO> nodes,
                                      int currentLevel,
                                      List<BizGroupCondition> conditions,
                                      Map<Long, String> bizDomainNameMap,
                                      Map<Long, String> productLineNameMap,
                                      Map<String, Map<String, String>> enumMap,
                                      Map<Long, Map<Long, String>> bizDomainMap,
                                      Map<Long, Map<Long, String>> lineMap) {
        // 如果当前层级是业务域分组
        BizGroupCondition groupCondition = conditions.get(currentLevel);
        String field = groupCondition.getFieldName();

        if (BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(field)) {
            // 遍历所有业务域，确保每个都出现在结果中
            for (Map.Entry<Long, String> entry : bizDomainNameMap.entrySet()) {
                String bizDomainIdStr = String.valueOf(entry.getKey());
                String bizDomainName = entry.getValue();

                // 检查该业务域是否已存在于结果中
                if (!mapResult.containsKey(bizDomainIdStr)) {
                    // 如果不存在，创建一个空的节点，total为0
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(BIZ_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(bizDomainIdStr);
                    node.setLabel(bizDomainName);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = bizDemandTransformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                bizDomainNameMap, bizDomainMap.get(entry.getKey()), null, null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    // 如果存在，确保其子节点也完整
                    ensureBizChildrenComplete(nodes, bizDomainIdStr, currentLevel, conditions,
                            bizDomainNameMap, bizDomainMap.get(entry.getKey()), enumMap, bizDomainMap, lineMap);
                }
            }
        } else if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(field)) {
            // 遍历所有产品线，确保每个都出现在结果中
            for (Map.Entry<Long, String> entry : productLineNameMap.entrySet()) {
                String productLineIdStr = String.valueOf(entry.getKey());
                String productLineName = entry.getValue();

                // 检查该产品线是否已存在于结果中
                if (!mapResult.containsKey(productLineIdStr)) {
                    // 如果不存在，创建一个空的节点，total为0
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(BIZ_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(productLineIdStr);
                    node.setLabel(productLineName);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = bizDemandTransformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                lineMap.get(entry.getKey()), productLineNameMap, null, null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    // 如果存在，确保其子节点也完整
                    ensureBizChildrenComplete(nodes, productLineIdStr, currentLevel, conditions,
                            lineMap.get(entry.getKey()), productLineNameMap, enumMap, bizDomainMap, lineMap);
                }
            }
        } else if (BizDemandGroupFieldEnum.PRIORITY.getGroupField().equals(field)) {
            // 遍历所有优先级，确保每个都出现在结果中
            enumMap.get(BizDemandGroupFieldEnum.PRIORITY.getGroupField()).forEach((priority, priorityName) -> {
                if (!mapResult.containsKey(priority)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(BIZ_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(priority);
                    node.setLabel(priorityName);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = bizDemandTransformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                bizDomainNameMap, productLineNameMap, null, null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    ensureBizChildrenComplete(nodes, priority, currentLevel, conditions,
                            bizDomainNameMap, productLineNameMap, enumMap, bizDomainMap, lineMap);
                }
            });
        } else if (BizDemandGroupFieldEnum.STATUS.getGroupField().equals(field)) {
            // 遍历所有状态，确保每个都出现在结果中
            enumMap.get(BizDemandGroupFieldEnum.STATUS.getGroupField()).forEach((status, statusName) -> {
                if (!mapResult.containsKey(status)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(BIZ_GROUP_FIELD_MAP.get(field));
                    node.setFieldValue(status);
                    node.setLabel(statusName);
                    node.setTotal(0L);

                    // 递归创建子节点
                    if (currentLevel + 1 < conditions.size()) {
                        Map<String, Object> emptyChildMap = new HashMap<>();
                        List<DemandGroupNodeVO> childNodes = bizDemandTransformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                                bizDomainNameMap, productLineNameMap, null, null, null, bizDomainMap, lineMap);
                        node.setChildren(childNodes);
                    }

                    nodes.add(node);
                } else {
                    ensureBizChildrenComplete(nodes, status, currentLevel, conditions,
                            bizDomainNameMap, productLineNameMap, enumMap, bizDomainMap, lineMap);
                }
            });
        }
    }

    // 确保现有节点的子节点完整
    private void ensureBizChildrenComplete(List<DemandGroupNodeVO> nodes, String fieldValue, int currentLevel,
                                           List<BizGroupCondition> conditions,
                                           Map<Long, String> bizDomainNameMap,
                                           Map<Long, String> productLineNameMap,
                                           Map<String, Map<String, String>> enumMap,
                                           Map<Long, Map<Long, String>> bizDomainMap,
                                           Map<Long, Map<Long, String>> lineMap) {
        for (DemandGroupNodeVO node : nodes) {
            if (fieldValue.equals(node.getFieldValue()) &&
                    (node.getChildren() == null || node.getChildren().isEmpty()) &&
                    currentLevel + 1 < conditions.size()) {

                Map<String, Object> emptyChildMap = new HashMap<>();
                List<DemandGroupNodeVO> childNodes = bizDemandTransformToTree(enumMap, emptyChildMap, currentLevel + 1, conditions,
                        bizDomainNameMap, productLineNameMap, null, null, null, bizDomainMap, lineMap);
                node.setChildren(childNodes);
            }
        }
    }

    private void addNotInLabelCategoryDemandGroupNode(Map<Long, Map<Long, String>> labelMaps, Long simpleGroupCount, List<DemandGroupNodeVO> roots) {
        DemandGroupNodeVO labelDemandGroupNodeVO = new DemandGroupNodeVO();
        labelDemandGroupNodeVO.setLabel("其他标签组");
        labelDemandGroupNodeVO.setField("notInLabelIds");
        // 从labelMaps中拿到所有标签ids
        labelDemandGroupNodeVO.setFieldValue(getAllLabelIdsFromMaps(labelMaps));
        labelDemandGroupNodeVO.setTotal(simpleGroupCount);
        roots.add(labelDemandGroupNodeVO);
    }

    /**
     * 从labelMaps中拿到所有标签ids
     */
    private String getAllLabelIdsFromMaps(Map<Long, Map<Long, String>> labelMaps) {
        return Optional.ofNullable(labelMaps)
                .orElse(Collections.emptyMap())
                .values()
                .stream()
                .filter(Objects::nonNull)
                .flatMap(map -> map.keySet().stream())
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.joining(","));
    }

    /**
     * 执行多级分组
     *
     * @param data
     * @param conditions 分组条件
     * @return
     */
    public Map<String, Object> group(List<ProductDemandGroupFieldDO> data,
                                     List<ProductGroupCondition> conditions) {
        // 构建动态Collector链
        Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>> collector =
                buildCollectorChain(conditions, 0);

        // 执行分组
        Map<String, Object> result = data.stream().collect(collector);

        // 如果需要对结果进行后处理，可以在这里添加
        return optimizeSingleOtherGroups(result);
    }

    /**
     * 对 type 维度进行多值拆分。后端 SQL 聚合时 type 可能以字符串形式返回（如 "[0,1]" 或 "0,1"），
     * 这里在进入分组逻辑前进行行级展开，使每个 type 值各自参与分组统计。
     */
    private List<ProductDemandGroupFieldDO> expandTypeMultiValues(List<ProductDemandGroupFieldDO> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            return rows;
        }
        List<ProductDemandGroupFieldDO> expanded = new ArrayList<>(rows.size());
        for (ProductDemandGroupFieldDO row : rows) {
            List<String> codes = parseTypeCodes(row.getType());
            if (CollectionUtils.isEmpty(codes)) {
                expanded.add(row);
                continue;
            }
            if (codes.size() == 1) {
                row.setType(codes.get(0));
                expanded.add(row);
                continue;
            }
            for (String code : codes) {
                ProductDemandGroupFieldDO copy = copyRow(row);
                copy.setType(code);
                expanded.add(copy);
            }
        }
        return expanded;
    }

    public Map<String, Object> bizDemandGroup(List<BizDemandGroupFieldDO> data,
                                              List<BizGroupCondition> conditions) {
        // 构建动态Collector链
        Collector<BizDemandGroupFieldDO, ?, Map<String, Object>> collector =
                buildBizDemandCollectorChain(conditions, 0);

        // 执行分组
        Map<String, Object> result = data.stream().collect(collector);
        // 如果需要对结果进行后处理，可以在这里添加
        return optimizeSingleOtherGroups(result);
    }

    private Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>> buildCollectorChain(
            List<ProductGroupCondition> conditions, int index) {

        if (index >= conditions.size()) {
            // 终止条件：返回计数Collector
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
        // 直接使用字段提取函数获取值
        Function<ProductDemandGroupFieldDO, String> extractor = item -> {
            Object rawValue = condition.getFieldExtractor().apply(item);
            // 直接转换为字符串，空值处理为"其他"
            return rawValue == null ? OTHER : String.valueOf(rawValue);
        };

        // 递归构建下一级分组器
        Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>> nextCollector =
                buildCollectorChain(conditions, index + 1);

        // 构建分组收集器
        Collector<ProductDemandGroupFieldDO, ?, Map<String, Map<String, Object>>> groupingCollector =
                Collectors.groupingBy(
                        extractor,
                        // 保持顺序
                        LinkedHashMap::new,
                        (Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>>) nextCollector
                );

        return (Collector<ProductDemandGroupFieldDO, ?, Map<String, Object>>) (Collector<?, ?, ?>) groupingCollector;
    }

    /**
     * 优化只包含"其他"子分组的节点结构，将只包含"其他"的嵌套结构扁平化
     *
     * @param groupedMap 分组结果
     * @return 优化后的结果
     */
    private Map<String, Object> optimizeSingleOtherGroups(Map<String, Object> groupedMap) {
        // 首先检查整个Map是否是 {其他={total=X}} 结构
        if (isOtherWithSingleTotal(groupedMap)) {
            return (Map<String, Object>) groupedMap.get(OTHER);
        }

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : groupedMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // 递归处理子Map
                Map<String, Object> processedValue = optimizeSingleOtherGroups((Map<String, Object>) value);

                // 检查处理后的子Map是否是 {其他={total=X}} 结构
                if (isOtherWithSingleTotal(processedValue)) {
                    result.put(key, processedValue.get(OTHER));
                } else {
                    result.put(key, processedValue);
                }
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    // 辅助方法：判断是否是 {其他={total=X}} 结构
    private boolean isOtherWithSingleTotal(Map<String, Object> map) {
        if (map.size() == 1 && map.containsKey(OTHER)) {
            Object otherValue = map.get(OTHER);
            if (otherValue instanceof Map) {
                Map<String, Object> otherMap = (Map<String, Object>) otherValue;
                return otherMap.size() == 1 && otherMap.containsKey("total");
            }
        }
        return false;
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
        // 直接使用字段提取函数获取值
        Function<BizDemandGroupFieldDO, String> extractor = item -> {
            Object rawValue = condition.getFieldExtractor().apply(item);
            // 直接转换为字符串，空值处理为"其他"
            return rawValue == null ? OTHER : String.valueOf(rawValue);
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
    public List<DemandGroupNodeVO> transformToTree(Map<String, Map<String, String>> enumMap,
                                                   Map<String, Object> mapResult,
                                                   int currentLevel,
                                                   List<ProductGroupCondition> conditions,
                                                   Map<Long, String> bizDomainNameMap,
                                                   Map<Long, String> productLineNameMap,
                                                   Map<String, String> ownerNameMap,
                                                   Map<Long, String> labelNameMap,
                                                   Map<Long, Map<Long, String>> bizDomainMap,
                                                   Map<Long, Map<Long, String>> lineMap) {
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
                label = OTHER.equals(fieldValue) ? OTHER : labelNameMap.get(Long.parseLong(fieldValue));
            }
            if (ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(field)) {
                if (OTHER.equals(fieldValue)) {
                    fieldValue = "";
                    label = OTHER;
                } else {
                    label = bizDomainNameMap.get(Long.parseLong(fieldValue));
                }
            }
            if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(field)) {
                if (OTHER.equals(fieldValue)) {
                    fieldValue = "";
                    label = OTHER;
                } else {
                    label = productLineNameMap.get(Long.parseLong(fieldValue));
                }
            }
            if (ProductGroupFieldEnum.OWNER.getGroupField().equals(field)) {
                label = ownerNameMap.getOrDefault(fieldValue, OTHER);
            }
            if (ProductGroupFieldEnum.STATUS.getGroupField().equals(field)) {
                label = ProductDemandStatusEnum.getTextByCode(Integer.parseInt(fieldValue));
            }
            if (ProductGroupFieldEnum.PRIORITY.getGroupField().equals(field)) {
                label = PriorityEnum.getTextByCode(Integer.parseInt(fieldValue));
            }
            if (ProductGroupFieldEnum.EXPECT_SCHEDULE_TIME.getGroupField().equals(field)) {
                label = StringUtils.isBlank(fieldValue) ? OTHER : fieldValue;
            }
            if (ProductGroupFieldEnum.TYPE.getGroupField().equals(field)) {
                label = StringUtils.defaultIfBlank(ProductDemandTypeEnum.getTextByCode(Integer.parseInt(fieldValue)), OTHER);
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
                    List<DemandGroupNodeVO> childNodes = transformToTree(enumMap, valueMap, currentLevel + 1, conditions, bizDomainNameMap, productLineNameMap, ownerNameMap, labelNameMap, bizDomainMap, lineMap);
                    node.setChildren(childNodes);
                    // 计算当前节点count（子节点count之和）
                    node.setTotal(childNodes.stream().mapToLong(DemandGroupNodeVO::getTotal).sum());
                }
            }
            nodes.add(node);
        }

        // 填充缺失的业务域和产品线节点
        fillAllGroupNodes(mapResult, nodes, currentLevel, conditions, bizDomainNameMap, productLineNameMap, enumMap, bizDomainMap, lineMap);

        return nodes;
    }

    public List<DemandGroupNodeVO> bizDemandTransformToTree(Map<String, Map<String, String>> enumMap,
                                                            Map<String, Object> mapResult,
                                                            int currentLevel,
                                                            List<BizGroupCondition> conditions,
                                                            Map<Long, String> bizDomainNameMap,
                                                            Map<Long, String> productLineNameMap,
                                                            Map<String, String> deptNameMap,
                                                            Map<String, String> receiveManNameMap,
                                                            Map<Long, String> labelNameMap,
                                                            Map<Long, Map<Long, String>> bizDomainMap,
                                                            Map<Long, Map<Long, String>> lineMap) {
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
                label = OTHER.equals(fieldValue) ? OTHER : labelNameMap.get(Long.parseLong(fieldValue));
            }
            if (BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(field)) {
                if (OTHER.equals(fieldValue)) {
                    fieldValue = "";
                    label = OTHER;
                } else {
                    label = bizDomainNameMap.get(Long.parseLong(fieldValue));
                }
            }
            if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(field)) {
                if (OTHER.equals(fieldValue)) {
                    fieldValue = "";
                    label = OTHER;
                } else {
                    label = productLineNameMap.get(Long.parseLong(fieldValue));
                }
            }
            if (BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField().equals(field)) {
                label = receiveManNameMap.getOrDefault(fieldValue, OTHER);
            }
            if (BizDemandGroupFieldEnum.DEMAND_DEPT.getGroupField().equals(field)) {
                label = deptNameMap.get(fieldValue);
            }
            if (BizDemandGroupFieldEnum.STATUS.getGroupField().equals(field)) {
                label = BizDemandStatusEnum.getTextByCode(Integer.parseInt(fieldValue));
            }
            if (BizDemandGroupFieldEnum.PRIORITY.getGroupField().equals(field)) {
                label = PriorityEnum.getTextByCode(Integer.parseInt(fieldValue));
            }
            if (BizDemandGroupFieldEnum.TARGET_CUSTOMER.getGroupField().equals(field)) {
                label = StringUtils.isBlank(fieldValue) ? OTHER : fieldValue;
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
                    List<DemandGroupNodeVO> childNodes = bizDemandTransformToTree(enumMap, valueMap, currentLevel + 1, conditions, bizDomainNameMap, productLineNameMap, deptNameMap, receiveManNameMap, labelNameMap, bizDomainMap, lineMap);
                    node.setChildren(childNodes);
                    // 计算当前节点count（子节点count之和）
                    node.setTotal(childNodes.stream().mapToLong(DemandGroupNodeVO::getTotal).sum());
                }
            }
            nodes.add(node);
        }

        // 填充缺失的业务域和产品线节点
        fillBizAllGroupNodes(mapResult, nodes, currentLevel, conditions, bizDomainNameMap, productLineNameMap, enumMap, bizDomainMap, lineMap);
        return nodes;
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
        List<Long> labelIds = parentCondition.getLabelIds();
        List<Long> notInLabelIds = parentCondition.getNotInLabelIds();
        if (CollUtil.isNotEmpty(labelCategoryIds) && (CollUtil.isNotEmpty(labelIds) || CollUtil.isNotEmpty(notInLabelIds))) {
            //查询等于标签id的需求
            List<Long> newLabelIds = getNewLabelIds(labelIds, labelCategoryIds, parentCondition.getNotInLabelIds());
            // 查询使用这些标签的需求id
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> ids = bizLabelDOList.stream().map(BizLabelDO::getBizId).distinct().collect(Collectors.toList());
            List<Long> bizIds;
            if (CollUtil.isNotEmpty(labelIds) && CollUtil.isNotEmpty(notInLabelIds)) {
                bizLabelDOList.addAll(bizLabelMapper.getByLabelIdInType(notInLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode()));
                // 获取包含labelIds且但不包含notInLabelIds的bizId
                bizIds = getLabelBizIds(labelIds, notInLabelIds, bizLabelDOList, ids);
            } else if (CollUtil.isNotEmpty(labelIds)) {
                // 取出bizLabelDOList中包含labelIds中所有标签id的bizId
                bizIds = getBizIdsContainingAllLabels(bizLabelDOList, labelIds);
            } else {
                // 查询没有使用这些标签的需求id
                bizIds = bizLabelMapper.getByLabelIdNotInType(notInLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode())
                        .stream()
                        .map(BizLabelDO::getBizId)
                        .collect(Collectors.toList());
            }

            if (CollectionUtils.isEmpty(bizIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }

            parentCondition.setInProductDemandIds(bizIds);
        }
        // 分页查询
        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);

        ProductDemandGroupQueryCondition groupCondition = ProductDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .build();

        List<ProductDemandListDO> productDemandListDO = productDemandComponent.getGroupList(groupCondition);

        PageQueryResult<ProductDemandVO> pageQueryResult = groupDuplicateUtil.getDemandVOQueryResultVO(productDemandListDO);
        return BaseResult.success(pageQueryResult);
    }

    /**
     * 取出bizLabelDOList中包含labelIds中所有标签id的bizId
     */
    private List<Long> getBizIdsContainingAllLabels(List<BizLabelDO> bizLabelDOList, List<Long> labelIds) {
        if (CollectionUtils.isEmpty(bizLabelDOList) || CollectionUtils.isEmpty(labelIds)) {
            return Collections.emptyList();
        }

        return bizLabelDOList.stream()
                .collect(Collectors.groupingBy(BizLabelDO::getBizId))
                .entrySet()
                .stream()
                .filter(entry -> {
                    Set<Long> labelIdsForBiz = entry.getValue().stream()
                            .map(BizLabelDO::getLabelId)
                            .collect(Collectors.toSet());
                    return labelIdsForBiz.containsAll(labelIds);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
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
        List<Long> queryListLabelIds = bizDemandQueryList.getLabelIds();
        List<Long> listLabelCategoryIds = bizDemandQueryList.getLabelCategoryIds();
        if (CollectionUtils.isNotEmpty(queryListLabelIds) || CollectionUtils.isNotEmpty(listLabelCategoryIds)) {
            List<Long> labelIds = labelComponent.getLabelIds(queryListLabelIds, listLabelCategoryIds);
            if (CollectionUtils.isEmpty(labelIds) && bizDemandQueryList.getContainLabel()) {
                return BaseResult.success(new ArrayList<>());
            }
            condition.setLabelIds(labelIds);
        }

        // 3) 名称字典一次性查询
        Map<String, String> deptNameMap = new HashMap<>();
        Map<Long, String> bizDomainNameMap;
        Map<Long, String> productLineNameMap;
        Map<String, String> receiveManNameMap = new HashMap<>();
        Map<Long, String> labelNameMap = new HashMap<>();

        long count = groupFields.stream().filter(groupField -> groupField.getType() == 1).count();
        List<Long> labelCategoryId = groupFields.stream()
                .filter(groupField -> groupField.getType() == 1)
                .map(groupField -> Long.valueOf(groupField.getKey()))
                .collect(Collectors.toList());
        Map<Long, Map<Long, String>> labelMaps = new HashMap<>(labelCategoryId.size());
        if (CollUtil.isNotEmpty(labelCategoryId)) {
            // 定义分组条件
            List<LabelDO> byCategoryIds = labelMapper.getByCategoryIds(labelCategoryId, false);
            // 根据不同的标签类别，得到以分组id为key,不同的标签Map集合的Map集合，key为标签id，value为标签名称
            labelMaps = byCategoryIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(LabelDO::getLabelCategoryId,
                            Collectors.toMap(LabelDO::getId, LabelDO::getName, (a, b) -> b)));
        }

        List<String> bizGroupFieldKeys = groupFields.stream().map(ViewsGroupQueryList::getKey).collect(Collectors.toList());

        List<Long> bizDomainIds = condition.getBizDemandIds();
        if (CollectionUtils.isNotEmpty(bizDomainIds)) {
            bizDomainNameMap = bizDomainMapper.getByIds(bizDomainIds).stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
        } else {
            bizDomainNameMap = bizDomainMapper.selectAllBizDomain().stream().collect(Collectors.toMap(BizDomainDO::getId, BizDomainDO::getName, (a, b) -> b));
        }

        List<Long> productLineIds = condition.getProductLineIdList();
        if (CollectionUtils.isNotEmpty(productLineIds)) {
            productLineNameMap = productLineMapper.getByIds(productLineIds).stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
        } else {
            productLineNameMap = productLineMapper.selectAllProductLine().stream().collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> b));
        }

        List<Long> bizDomainIdList = new ArrayList<>(bizDomainNameMap.keySet());
        List<Long> productLineIdList = new ArrayList<>(productLineNameMap.keySet());
        List<ProjectProductLineBizDomain> plineAndBizDomain = productLineMapper.getPlineAndBizDomainList(bizDomainIdList, productLineIdList);
        Map<Long, Map<Long, String>> bizDomainMap = plineAndBizDomain
                .stream()
                .collect(Collectors.groupingBy(
                        ProjectProductLineBizDomain::getBizDomainId,
                        Collectors.toMap(
                                ProjectProductLineBizDomain::getProductLineId,
                                ProjectProductLineBizDomain::getProductLineName,
                                // 处理重复键的情况
                                (existing, replacement) -> existing
                        )
                ));
        Map<Long, Map<Long, String>> lineMap = plineAndBizDomain
                .stream()
                .collect(Collectors.groupingBy(
                        ProjectProductLineBizDomain::getProductLineId,
                        Collectors.toMap(
                                ProjectProductLineBizDomain::getBizDomainId,
                                ProjectProductLineBizDomain::getBizDomainName,
                                (existing, replacement) -> existing
                        )
                ));

        Map<String, Map<String, String>> bizEnumMap = getBizEnumMap(condition);

        if (count > 1) {
            parentCondition.setLabelCategoryIds(labelCategoryId);
            BizDemandGroupQueryCondition groupCondition = BizDemandGroupQueryCondition.builder()
                    .condition(condition)
                    .parentCondition(parentCondition)
                    .build();
            List<BizDemandGroupFieldDO> simpleGroupList = bizDemandComponent.getSimpleGroupList(groupCondition);

            // 查询不在标签类别的需求数
            parentCondition.setLabelCategoryIds(null);
            parentCondition.setNotInLabelCategoryIds(labelCategoryId);
            BizDemandGroupQueryCondition groupCountCondition = BizDemandGroupQueryCondition.builder()
                    .condition(condition)
                    .parentCondition(parentCondition)
                    .build();
            Long simpleGroupCount = bizDemandComponent.getSimpleGroupCount(groupCountCondition);

            if (CollUtil.isEmpty(simpleGroupList)) {
                simpleGroupList.add(new BizDemandGroupFieldDO());
            }

            if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField())) {
                receiveManNameMap = getPersonMap(receiveManNameMap, simpleGroupList.stream().map(BizDemandGroupFieldDO::getReceiveManId));
            }

            deptNameMap = getDemandDeptMap(deptNameMap, simpleGroupList, bizGroupFieldKeys);

            if (groupFields.stream().anyMatch(e -> e.getType() == 1)) {
                labelNameMap = getLabelMap(labelNameMap, simpleGroupList.stream().map(BizDemandGroupFieldDO::getLabelId));
            }

            List<BizGroupCondition> conditions = new ArrayList<>(5);

            // 记录标签字段的索引
            int labelFieldIndex = 1;

            for (int i = 0; i < groupFields.size(); i++) {
                if (groupFields.get(i).getType() == 1) {
                    // 标签类别层：使用对应的 label1-label5 字段提取函数
                    String fieldName = ProductGroupFieldEnum.LABEL.getGroupField() + labelFieldIndex;
                    Function<BizDemandGroupFieldDO, Object> labelExtractor = BIZ_FUNCTION_FIELD_MAP.get(fieldName);
                    conditions.add(new BizGroupCondition(
                            fieldName,
                            labelExtractor
                    ));
                    labelFieldIndex++;
                } else {
                    conditions.add(new BizGroupCondition(groupFields.get(i).getKey(), BIZ_FUNCTION_FIELD_MAP.get(groupFields.get(i).getKey())));
                }
            }

            // 根据分组条件重新组装数据，填充 label1-label5 字段
            List<BizDemandGroupFieldDO> dataForGrouping = populateBizLabelFields(simpleGroupList, groupFields, labelMaps);

            Map<String, Object> groupResult = bizDemandGroup(dataForGrouping, conditions);

            // 从Map<Long, Map<Long, String>>中提取所有的值，构建成Map<Long, String>集合
            bizDomainNameMap = lineMap.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(innerMap -> innerMap.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            // 处理key冲突，保留第一个值
                            (v1, v2) -> v1
                    ));

            // 从Map<Long, Map<Long, String>>中提取所有的值，构建成Map<Long, String>集合
            productLineNameMap = bizDomainMap.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(innerMap -> innerMap.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            // 处理key冲突，保留第一个值
                            (v1, v2) -> v1
                    ));
            // 转换方法1：基础转换
            List<DemandGroupNodeVO> treeNodes = bizDemandTransformToTree(bizEnumMap, groupResult, 0, conditions, bizDomainNameMap, productLineNameMap, deptNameMap, receiveManNameMap, labelNameMap, bizDomainMap, lineMap);

            // 排序（各层按 label 升序）
            sortTreeByLabel(treeNodes);

            addNotInLabelCategoryDemandGroupNode(labelMaps, simpleGroupCount, treeNodes);
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
        // 重新赋值
        bizGroupFieldKeys = groupFields.stream().map(ViewsGroupQueryList::getKey).collect(Collectors.toList());

        BizDemandGroupQueryCondition groupCondition = BizDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .selectField(selectField)
                .groupField(groupField)
                .build();

        List<BizDemandGroupFieldDO> rows = bizDemandComponent.groupTree(groupCondition);

        Long simpleGroupCount = 0L;
        if (count > 0) {
            // 查询不在标签类别的需求数
            parentCondition.setNotInLabelCategoryIds(parentCondition.getLabelCategoryIds());
            parentCondition.setLabelCategoryIds(null);
            BizDemandGroupQueryCondition groupCountCondition = BizDemandGroupQueryCondition.builder()
                    .condition(condition)
                    .parentCondition(parentCondition)
                    .build();
            simpleGroupCount = bizDemandComponent.getSimpleGroupCount(groupCountCondition);
        }

        if (CollUtil.isEmpty(rows)) {
            rows.add(new BizDemandGroupFieldDO());
        }

        if (bizGroupFieldKeys.contains(BizDemandGroupFieldEnum.RECEIVE_MAN.getGroupField())) {
            receiveManNameMap = getPersonMap(receiveManNameMap, rows.stream().map(BizDemandGroupFieldDO::getReceiveManId));
        }
        if (groupFields.stream().anyMatch(e -> e.getType() == 1)) {
            labelNameMap = getLabelMap(labelNameMap, rows.stream().map(BizDemandGroupFieldDO::getLabelId));
        }
        deptNameMap = getDemandDeptMap(deptNameMap, rows, bizGroupFieldKeys);

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

        // 在节点构建完成后，对根节点进行完整填充
        fillBizMissingGroupNodes(roots, bizGroupFieldKeys, bizDomainMap, lineMap, bizEnumMap);

        // 排序（各层按 label 升序）
        sortTreeByLabel(roots);

        if (count > 0) {
            addNotInLabelCategoryDemandGroupNode(labelMaps, simpleGroupCount, roots);
        }
        return BaseResult.success(roots);
    }

    private Map<String, String> getDemandDeptMap(Map<String, String> deptNameMap, List<BizDemandGroupFieldDO> simpleGroupList, List<String> bizGroupFieldKeys) {
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
        return deptNameMap;
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
        List<Long> queryListLabelIds = bizDemandQueryList.getLabelIds();
        List<Long> listLabelCategoryIds = bizDemandQueryList.getLabelCategoryIds();
        if (CollectionUtils.isNotEmpty(queryListLabelIds) || CollectionUtils.isNotEmpty(listLabelCategoryIds)) {
            List<Long> labelIds = labelComponent.getLabelIds(queryListLabelIds, listLabelCategoryIds);
            if (CollectionUtils.isEmpty(labelIds) && BooleanUtils.isTrue(bizDemandQueryList.getContainLabel())) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setLabelIds(labelIds);
        }

        // 获取标签类别Id
        List<Long> labelCategoryIds = groupFields.stream().filter(field -> field.getType() == 1).map(field -> Long.valueOf(field.getKey())).collect(Collectors.toList());
        List<Long> notInLabelIds = parentCondition.getNotInLabelIds();
        List<Long> labelIds = parentCondition.getLabelIds();
        if (CollUtil.isNotEmpty(labelCategoryIds) && (CollUtil.isNotEmpty(labelIds) || CollUtil.isNotEmpty(notInLabelIds))) {
            //查询等于标签id的需求
            List<Long> newLabelIds = getNewLabelIds(labelIds, labelCategoryIds, parentCondition.getNotInLabelIds());
            // 查询使用这些标签的需求id
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.BIZ_DEMAND.getCode());
            List<Long> ids = bizLabelDOList.stream().map(BizLabelDO::getBizId).distinct().collect(Collectors.toList());
            List<Long> bizIds;
            if (CollUtil.isNotEmpty(labelIds) && CollUtil.isNotEmpty(notInLabelIds)) {
                bizLabelDOList.addAll(bizLabelMapper.getByLabelIdInType(notInLabelIds, BizTypeEnum.BIZ_DEMAND.getCode()));
                // 获取包含labelIds且但不包含notInLabelIds的bizId
                bizIds = getLabelBizIds(labelIds, notInLabelIds, bizLabelDOList, ids);
            } else if (CollUtil.isNotEmpty(labelIds)) {
                // 取出bizLabelDOList中包含labelIds中所有标签id的bizId
                bizIds = getBizIdsContainingAllLabels(bizLabelDOList, labelIds);
            } else {
                // 查询没有使用这些标签的需求id
                bizIds = bizLabelMapper.getByLabelIdNotInType(notInLabelIds, BizTypeEnum.BIZ_DEMAND.getCode())
                        .stream()
                        .map(BizLabelDO::getBizId)
                        .collect(Collectors.toList());
            }

            if (CollectionUtils.isEmpty(bizIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }

            parentCondition.setContainIds(bizIds);
        }

        BizDemandGroupQueryCondition groupCondition = BizDemandGroupQueryCondition.builder()
                .condition(condition)
                .parentCondition(parentCondition)
                .groupField(null)
                .build();

        PageQueryResult<BizDemandVO> pageQueryResult = bizDemandComponent.groupList(groupCondition);

        if (bizDemandQueryList.getQuerySource() == 1) {
            // 交付项目来源查询需要特殊排序
            List<BizDemandVO> resultList = pageQueryResult.getResultList();
            groupDuplicateUtil.sortByCreateDate(resultList);
        }

        return BaseResult.success(pageQueryResult);
    }

    private List<Long> getLabelBizIds(List<Long> labelIds, List<Long> notInLabelIds, List<BizLabelDO> bizLabelDOList, List<Long> ids) {
        return bizLabelDOList.stream()
                .collect(Collectors.groupingBy(BizLabelDO::getBizId))
                .entrySet()
                .stream()
                .filter(entry -> {
                    Set<Long> labelIdsForBiz = entry.getValue().stream()
                            .map(BizLabelDO::getLabelId)
                            .collect(Collectors.toSet());

                    // 检查是否包含所有labelIds且不包含任何notInLabelIds
                    return labelIdsForBiz.containsAll(labelIds) &&
                            Collections.disjoint(labelIdsForBiz, notInLabelIds);
                })
                .map(Map.Entry::getKey)
                // 确保结果只包含原先bizIds中的元素
                .filter(ids::contains)
                .collect(Collectors.toList());
    }

    private List<Long> getNewLabelIds(List<Long> labelIds, List<Long> labelCategoryIds, List<Long> notInLabelIds) {
        //查询等于标签id的需求
        List<Long> newLabelIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(labelIds)) {
            newLabelIds = labelComponent.getLabelIds(labelIds, null);
        }

        //查询不等于标签id的需求
        List<Long> newNotInLabelIds;
        if (CollUtil.isNotEmpty(notInLabelIds)) {
            newNotInLabelIds = labelMapper.getByCategoryIds(labelCategoryIds, false).stream().map(LabelDO::getId).collect(Collectors.toList());
            newNotInLabelIds.removeAll(notInLabelIds);
            newLabelIds.retainAll(newNotInLabelIds);
        }
        return newLabelIds;
    }

    private void sortTreeByLabel(List<DemandGroupNodeVO> nodes) {
        if (CollectionUtils.isEmpty(nodes)) {
            return;
        }
        // 处理其他
        String values = nodes.stream().filter(n -> !OTHER.equals(n.getLabel())).map(DemandGroupNodeVO::getFieldValue).collect(Collectors.joining(","));
        List<DemandGroupNodeVO> removeNodes = new ArrayList<>();
        for (DemandGroupNodeVO node : nodes) {
            if (OTHER.equals(node.getLabel()) && "label".equals(node.getField())) {
                node.setFieldValue(StringUtils.isNotBlank(values) ? values : "-1");
                node.setField(QUERY_OTHER_FIELD_MAP.get(node.getField()));
            }
            if (StringUtils.isBlank(node.getFieldValue())) {
                removeNodes.add(node);
            }
        }
        nodes.removeAll(removeNodes);
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

    // 新增方法：填充缺失的分组节点
    private void fillMissingGroupNodes(List<DemandGroupNodeVO> roots,
                                       List<String> groupFields,
                                       Map<Long, Map<Long, String>> bizDomainPLineMap,
                                       Map<Long, Map<Long, String>> productLineDomMap,
                                       Map<String, Map<String, String>> enumMap) {
        if (CollectionUtils.isEmpty(groupFields) || CollectionUtils.isEmpty(roots)) {
            return;
        }

        String firstGroupField = groupFields.get(0);

        // 从Map<Long, Map<Long, String>>中提取所有的值，构建成Map<Long, String>集合
        Map<Long, String> bizDomainNameMap = productLineDomMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(innerMap -> innerMap.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        // 处理key冲突，保留第一个值
                        (v1, v2) -> v1
                ));

        // 从Map<Long, Map<Long, String>>中提取所有的值，构建成Map<Long, String>集合
        Map<Long, String> productLineNameMap = bizDomainPLineMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(innerMap -> innerMap.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        // 处理key冲突，保留第一个值
                        (v1, v2) -> v1
                ));

        // 创建现有的值集合
        Set<String> existingValues = roots.stream()
                .map(DemandGroupNodeVO::getFieldValue)
                .collect(Collectors.toSet());

        // 根据第一层分组字段类型填充缺失节点
        if (ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(firstGroupField)) {
            for (Map.Entry<Long, String> entry : bizDomainNameMap.entrySet()) {
                String bizDomainId = String.valueOf(entry.getKey());
                if (!existingValues.contains(bizDomainId)) {
                    Map<Long, String> map = bizDomainPLineMap.get(entry.getKey());
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(bizDomainId);
                    node.setLabel(entry.getValue());
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createEmptyChildNodes(node, groupFields, 1, bizDomainNameMap, map, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            }
        } else if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(firstGroupField)) {
            for (Map.Entry<Long, String> entry : productLineNameMap.entrySet()) {
                String productLineId = String.valueOf(entry.getKey());
                if (!existingValues.contains(productLineId)) {
                    Map<Long, String> map = productLineDomMap.get(entry.getKey());
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(productLineId);
                    node.setLabel(entry.getValue());
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createEmptyChildNodes(node, groupFields, 1, map, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            }
        } else if (ProductGroupFieldEnum.PRIORITY.getGroupField().equals(firstGroupField)) {
            enumMap.get(ProductGroupFieldEnum.PRIORITY.getGroupField()).forEach((priorityCode, priorityText) -> {
                if (!existingValues.contains(priorityCode)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(priorityCode);
                    node.setLabel(priorityText);
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createEmptyChildNodes(node, groupFields, 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            });
        } else if (ProductGroupFieldEnum.STATUS.getGroupField().equals(firstGroupField)) {
            enumMap.get(ProductGroupFieldEnum.STATUS.getGroupField()).forEach((statusCode, statusText) -> {
                if (!existingValues.contains(statusCode)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(statusText);
                    node.setLabel(statusText);
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createEmptyChildNodes(node, groupFields, 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            });
        } else if (ProductGroupFieldEnum.TYPE.getGroupField().equals(firstGroupField)) {
            enumMap.get(ProductGroupFieldEnum.TYPE.getGroupField()).forEach((typeCode, typeText) -> {
                if (!existingValues.contains(typeCode)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(PRODUCT_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(typeCode);
                    node.setLabel(typeText);
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createEmptyChildNodes(node, groupFields, 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            });
        }

        // 对现有节点也确保子级结构完整
        for (DemandGroupNodeVO node : roots) {
            ensureChildNodesComplete(node, groupFields, 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
        }
    }

    private void fillBizMissingGroupNodes(List<DemandGroupNodeVO> roots,
                                          List<String> groupFields,
                                          Map<Long, Map<Long, String>> bizDomainPLineMap,
                                          Map<Long, Map<Long, String>> productLineDomMap,
                                          Map<String, Map<String, String>> enumMap) {
        if (CollectionUtils.isEmpty(groupFields) || CollectionUtils.isEmpty(roots)) {
            return;
        }

        String firstGroupField = groupFields.get(0);

        // 从Map<Long, Map<Long, String>>中提取所有的值，构建成Map<Long, String>集合
        Map<Long, String> bizDomainNameMap = productLineDomMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(innerMap -> innerMap.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        // 处理key冲突，保留第一个值
                        (v1, v2) -> v1
                ));

        // 从Map<Long, Map<Long, String>>中提取所有的值，构建成Map<Long, String>集合
        Map<Long, String> productLineNameMap = bizDomainPLineMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(innerMap -> innerMap.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        // 处理key冲突，保留第一个值
                        (v1, v2) -> v1
                ));

        // 创建现有的值集合
        Set<String> existingValues = roots.stream()
                .map(DemandGroupNodeVO::getFieldValue)
                .collect(Collectors.toSet());

        // 根据第一层分组字段类型填充缺失节点
        if (BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(firstGroupField)) {
            for (Map.Entry<Long, String> entry : bizDomainNameMap.entrySet()) {
                String bizDomainId = String.valueOf(entry.getKey());
                if (!existingValues.contains(bizDomainId)) {
                    Map<Long, String> map = bizDomainPLineMap.get(entry.getKey());
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(BIZ_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(bizDomainId);
                    node.setLabel(entry.getValue());
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createBizEmptyChildNodes(node, groupFields, 1, bizDomainNameMap, map, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            }
        } else if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(firstGroupField)) {
            for (Map.Entry<Long, String> entry : productLineNameMap.entrySet()) {
                String productLineId = String.valueOf(entry.getKey());
                if (!existingValues.contains(productLineId)) {
                    Map<Long, String> map = productLineDomMap.get(entry.getKey());
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(BIZ_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(productLineId);
                    node.setLabel(entry.getValue());
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createBizEmptyChildNodes(node, groupFields, 1, map, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            }
        } else if (BizDemandGroupFieldEnum.PRIORITY.getGroupField().equals(firstGroupField)) {
            enumMap.get(BizDemandGroupFieldEnum.PRIORITY.getGroupField()).forEach((priorityCode, priorityText) -> {
                if (!existingValues.contains(priorityCode)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(BIZ_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(priorityCode);
                    node.setLabel(priorityText);
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createBizEmptyChildNodes(node, groupFields, 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            });
        } else if (BizDemandGroupFieldEnum.STATUS.getGroupField().equals(firstGroupField)) {
            enumMap.get(BizDemandGroupFieldEnum.STATUS.getGroupField()).forEach((status, statusName) -> {
                if (!existingValues.contains(status)) {
                    DemandGroupNodeVO node = new DemandGroupNodeVO();
                    node.setField(BIZ_GROUP_FIELD_MAP.get(firstGroupField));
                    node.setFieldValue(status);
                    node.setLabel(statusName);
                    node.setTotal(0L);
                    // 为子级创建空节点结构
                    createBizEmptyChildNodes(node, groupFields, 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
                    roots.add(node);
                }
            });
        }

        // 对现有节点也确保子级结构完整
        for (DemandGroupNodeVO node : roots) {
            ensureBizChildNodesComplete(node, groupFields, 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
        }
    }

    // 创建空的子节点结构
    private void createEmptyChildNodes(DemandGroupNodeVO parent,
                                       List<String> groupFields,
                                       int level,
                                       Map<Long, String> bizDomainNameMap,
                                       Map<Long, String> productLineNameMap,
                                       Map<String, Map<String, String>> enumMap,
                                       Map<Long, Map<Long, String>> bizDomainPLineMap,
                                       Map<Long, Map<Long, String>> productLineDomMap) {
        if (level >= groupFields.size()) {
            return;
        }

        // 创建一层空的子节点
        List<DemandGroupNodeVO> children = parent.getChildren();
        String groupField = groupFields.get(level);

        List<DemandGroupNodeVO> parentChildren = parent.getChildren();
        Map<String, Long> longMap = Optional.ofNullable(parentChildren)
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        DemandGroupNodeVO::getFieldValue,
                        DemandGroupNodeVO::getTotal,
                        (existing, replacement) -> existing
                ));

        // 根据分组字段类型创建相应的空节点
        if (ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            for (Map.Entry<Long, String> entry : bizDomainNameMap.entrySet()) {
                DemandGroupNodeVO child = getChild(PRODUCT_GROUP_FIELD_MAP, entry, groupField, longMap, children);
                Map<Long, String> map = bizDomainPLineMap.get(entry.getKey());
                // 递归创建更深层的子节点
                createEmptyChildNodes(child, groupFields, level + 1, bizDomainNameMap, map, enumMap, bizDomainPLineMap, productLineDomMap);
            }
        } else if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            for (Map.Entry<Long, String> entry : productLineNameMap.entrySet()) {
                DemandGroupNodeVO child = getChild(PRODUCT_GROUP_FIELD_MAP, entry, groupField, longMap, children);
                Map<Long, String> map = productLineDomMap.get(entry.getKey());
                // 递归创建更深层的子节点
                createEmptyChildNodes(child, groupFields, level + 1, map, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            }
        } else if (ProductGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            enumMap.get(ProductGroupFieldEnum.PRIORITY.getGroupField()).forEach((priorityCode, priorityText) -> {
                DemandGroupNodeVO child = getEnumChild(PRODUCT_GROUP_FIELD_MAP, priorityCode, priorityText, groupField, longMap, children);
                // 递归创建更深层的子节点
                createEmptyChildNodes(child, groupFields, level + 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            });
        } else if (ProductGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            enumMap.get(ProductGroupFieldEnum.STATUS.getGroupField()).forEach((statusCode, statusText) -> {
                DemandGroupNodeVO child = getEnumChild(PRODUCT_GROUP_FIELD_MAP, statusCode, statusText, groupField, longMap, children);
                // 递归创建更深层的子节点
                createEmptyChildNodes(child, groupFields, level + 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            });
        } else if (ProductGroupFieldEnum.TYPE.getGroupField().equals(groupField)) {
            enumMap.get(ProductGroupFieldEnum.TYPE.getGroupField()).forEach((typeCode, typeText) -> {
                DemandGroupNodeVO child = getEnumChild(PRODUCT_GROUP_FIELD_MAP, typeCode, typeText, groupField, longMap, children);
                // 递归创建更深层的子节点
                createEmptyChildNodes(child, groupFields, level + 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            });
        }

        parent.setChildren(children);
    }

    private DemandGroupNodeVO getEnumChild(Map<String, String> fieldMap,
                                           String code,
                                           String text,
                                           String groupField, Map<String, Long> longMap,
                                           List<DemandGroupNodeVO> children) {
        DemandGroupNodeVO child = new DemandGroupNodeVO();
        Map<String, DemandGroupNodeVO> nodeVOMap = children.stream().collect(Collectors.toMap(DemandGroupNodeVO::getFieldValue, Function.identity()));
        if (!longMap.containsKey(code)) {
            child.setField(fieldMap.get(groupField));
            child.setFieldValue(code);
            child.setLabel(text);
            child.setTotal(0L);
            children.add(child);
        } else {
            child = nodeVOMap.get(code);
        }
        return child;
    }

    private DemandGroupNodeVO getChild(Map<String, String> fieldMap,
                                       Map.Entry<Long, String> entry,
                                       String groupField,
                                       Map<String, Long> longMap,
                                       List<DemandGroupNodeVO> children) {
        DemandGroupNodeVO child = new DemandGroupNodeVO();
        String valueOf = String.valueOf(entry.getKey());
        Map<String, DemandGroupNodeVO> nodeVOMap = children.stream().collect(Collectors.toMap(DemandGroupNodeVO::getFieldValue, Function.identity()));
        if (!longMap.containsKey(valueOf)) {
            child.setField(fieldMap.get(groupField));
            child.setFieldValue(valueOf);
            child.setLabel(entry.getValue());
            child.setTotal(0L);
            children.add(child);
        } else {
            child = nodeVOMap.get(valueOf);
        }
        return child;
    }

    private void createBizEmptyChildNodes(DemandGroupNodeVO parent,
                                          List<String> groupFields,
                                          int level,
                                          Map<Long, String> bizDomainNameMap,
                                          Map<Long, String> productLineNameMap,
                                          Map<String, Map<String, String>> enumMap,
                                          Map<Long, Map<Long, String>> bizDomainPLineMap,
                                          Map<Long, Map<Long, String>> productLineDomMap) {
        if (level >= groupFields.size()) {
            return;
        }

        // 创建一层空的子节点
        List<DemandGroupNodeVO> children = parent.getChildren();
        String groupField = groupFields.get(level);

        List<DemandGroupNodeVO> parentChildren = parent.getChildren();
        Map<String, Long> longMap = Optional.ofNullable(parentChildren)
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        DemandGroupNodeVO::getFieldValue,
                        DemandGroupNodeVO::getTotal,
                        (existing, replacement) -> existing
                ));

        // 根据分组字段类型创建相应的空节点
        if (BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            for (Map.Entry<Long, String> entry : bizDomainNameMap.entrySet()) {
                DemandGroupNodeVO child = getChild(BIZ_GROUP_FIELD_MAP, entry, groupField, longMap, children);
                Map<Long, String> map = bizDomainPLineMap.get(entry.getKey());
                // 递归创建更深层的子节点
                createBizEmptyChildNodes(child, groupFields, level + 1, bizDomainNameMap, map, enumMap, bizDomainPLineMap, productLineDomMap);
            }
        } else if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            for (Map.Entry<Long, String> entry : productLineNameMap.entrySet()) {
                DemandGroupNodeVO child = getChild(BIZ_GROUP_FIELD_MAP, entry, groupField, longMap, children);
                Map<Long, String> map = productLineDomMap.get(entry.getKey());
                // 递归创建更深层的子节点
                createBizEmptyChildNodes(child, groupFields, level + 1, map, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            }
        } else if (BizDemandGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            enumMap.get(BizDemandGroupFieldEnum.PRIORITY.getGroupField()).forEach((priorityCode, priorityText) -> {
                DemandGroupNodeVO child = getEnumChild(BIZ_GROUP_FIELD_MAP, priorityCode, priorityText, groupField, longMap, children);
                // 递归创建更深层的子节点
                createBizEmptyChildNodes(child, groupFields, level + 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            });
        } else if (BizDemandGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            enumMap.get(BizDemandGroupFieldEnum.STATUS.getGroupField()).forEach((statusCode, statusText) -> {
                DemandGroupNodeVO child = getEnumChild(BIZ_GROUP_FIELD_MAP, statusCode, statusText, groupField, longMap, children);
                // 递归创建更深层的子节点
                createBizEmptyChildNodes(child, groupFields, level + 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            });
        }

        parent.setChildren(children);
    }

    // 确保现有节点的子级结构完整
    private void ensureChildNodesComplete(DemandGroupNodeVO node,
                                          List<String> groupFields,
                                          int level,
                                          Map<Long, String> bizDomainNameMap,
                                          Map<Long, String> productLineNameMap,
                                          Map<String, Map<String, String>> enumMap,
                                          Map<Long, Map<Long, String>> bizDomainPLineMap,
                                          Map<Long, Map<Long, String>> productLineDomMap
    ) {
        if (level >= groupFields.size()) {
            return;
        }

        String group = groupFields.get(level);

        // 如果节点没有子节点，则创建空的子节点结构
        if (isChildNodesComplete(node, group, bizDomainNameMap, productLineNameMap, enumMap)) {
            createEmptyChildNodes(node, groupFields, level, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
        } else {
            // 递归处理子节点
            for (DemandGroupNodeVO child : node.getChildren()) {
                ensureChildNodesComplete(child, groupFields, level + 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            }
        }
    }

    /**
     * 检查节点的子节点是否完整（是否缺少某些分组项）
     *
     * @param node               当前节点
     * @param groupField         分组字段
     * @param bizDomainNameMap   业务域映射
     * @param productLineNameMap 产品线映射
     * @param enumMap            枚举映射
     * @return 如果子节点完整返回true，否则返回false
     */
    private boolean isChildNodesComplete(DemandGroupNodeVO node,
                                         String groupField,
                                         Map<Long, String> bizDomainNameMap,
                                         Map<Long, String> productLineNameMap,
                                         Map<String, Map<String, String>> enumMap) {
        // 检查节点或其子节点是否为null
        if (node == null) {
            return false;
        }

        if (node.getChildren() == null) {
            node.setChildren(new ArrayList<>());
        }

        // 根据不同的分组字段检查子节点数量是否完整
        if (ProductGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            return bizDomainNameMap.isEmpty() || node.getChildren().size() >= bizDomainNameMap.size();
        }

        if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            return productLineNameMap.isEmpty() || node.getChildren().size() >= productLineNameMap.size();
        }

        if (ProductGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            Map<String, String> priorityMap = enumMap.get(ProductGroupFieldEnum.PRIORITY.getGroupField());
            return MapUtils.isEmpty(priorityMap) || node.getChildren().size() >= priorityMap.size();
        }

        if (ProductGroupFieldEnum.TYPE.getGroupField().equals(groupField)) {
            Map<String, String> typeMap = enumMap.get(ProductGroupFieldEnum.TYPE.getGroupField());
            return MapUtils.isEmpty(typeMap) || node.getChildren().size() >= typeMap.size();
        }

        if (ProductGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            Map<String, String> statusMap = enumMap.get(ProductGroupFieldEnum.STATUS.getGroupField());
            return MapUtils.isEmpty(statusMap) || node.getChildren().size() < statusMap.size();
        }

        return false;
    }

    private boolean isBizChildNodesComplete(DemandGroupNodeVO node,
                                         String groupField,
                                         Map<Long, String> bizDomainNameMap,
                                         Map<Long, String> productLineNameMap,
                                         Map<String, Map<String, String>> enumMap) {
        // 检查节点或其子节点是否为null
        if (node == null) {
            return false;
        }

        if (node.getChildren() == null) {
            node.setChildren(new ArrayList<>());
        }

        // 根据不同的分组字段检查子节点数量是否完整
        if (BizDemandGroupFieldEnum.BIZ_DOMAIN.getGroupField().equals(groupField)) {
            return bizDomainNameMap.isEmpty() || node.getChildren().size() >= bizDomainNameMap.size();
        }

        if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            return productLineNameMap.isEmpty() || node.getChildren().size() >= productLineNameMap.size();
        }

        if (BizDemandGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            Map<String, String> priorityMap = enumMap.get(ProductGroupFieldEnum.PRIORITY.getGroupField());
            return MapUtils.isEmpty(priorityMap) || node.getChildren().size() >= priorityMap.size();
        }

        if (BizDemandGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            Map<String, String> statusMap = enumMap.get(ProductGroupFieldEnum.STATUS.getGroupField());
            return MapUtils.isEmpty(statusMap) || node.getChildren().size() < statusMap.size();
        }

        return false;
    }

    private void ensureBizChildNodesComplete(DemandGroupNodeVO node,
                                             List<String> groupFields,
                                             int level,
                                             Map<Long, String> bizDomainNameMap,
                                             Map<Long, String> productLineNameMap,
                                             Map<String, Map<String, String>> enumMap,
                                             Map<Long, Map<Long, String>> bizDomainPLineMap,
                                             Map<Long, Map<Long, String>> productLineDomMap) {
        if (level >= groupFields.size()) {
            return;
        }
        String group = groupFields.get(level);

        // 如果节点没有子节点，则创建空的子节点结构
        if (isBizChildNodesComplete(node, group, bizDomainNameMap, productLineNameMap, enumMap)) {
            createBizEmptyChildNodes(node, groupFields, level, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
        } else {
            // 递归处理子节点
            for (DemandGroupNodeVO child : node.getChildren()) {
                ensureBizChildNodesComplete(child, groupFields, level + 1, bizDomainNameMap, productLineNameMap, enumMap, bizDomainPLineMap, productLineDomMap);
            }
        }
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
        // 处理 label1-label5 字段
        if (groupField.startsWith(ProductGroupFieldEnum.LABEL.getGroupField())) {
            String suffix = groupField.substring(ProductGroupFieldEnum.LABEL.getGroupField().length());
            try {
                int index = Integer.parseInt(suffix);
                Long labelId = getLabelFieldValue(row, index);
                return labelId == null ? "" : String.valueOf(labelId);
            } catch (NumberFormatException e) {
                return "";
            }
        }
        return "";
    }

    /**
     * 获取指定位置的 label 字段值
     */
    private Long getLabelFieldValue(ProductDemandGroupFieldDO row, int position) {
        switch (position) {
            case 0:
                return row.getLabelId();
            case 1:
                return row.getLabelId1();
            case 2:
                return row.getLabelId2();
            case 3:
                return row.getLabelId3();
            case 4:
                return row.getLabelId4();
            case 5:
                return row.getLabelId5();
            default:
                return null;
        }
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
            if (StringUtils.isBlank(value)) return null;
            return bizDomainNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (ProductGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return null;
            return productLineNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (ProductGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return null;
            return ProductDemandStatusEnum.getTextByCode(Integer.parseInt(value));
        }
        if (ProductGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return null;
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
            if (StringUtils.isBlank(value)) return null;
            return StringUtils.defaultIfBlank(ProductDemandTypeEnum.getTextByCode(Integer.parseInt(value)), OTHER);
        }
        if (ProductGroupFieldEnum.LABEL.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return OTHER;
            return labelNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        // 处理 label1-label5 字段
        if (groupField.startsWith(ProductGroupFieldEnum.LABEL.getGroupField())) {
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
            if (StringUtils.isBlank(value)) return null;
            return bizDomainNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (BizDemandGroupFieldEnum.PRODUCT_LINE.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return null;
            return productLineNameMap.getOrDefault(Long.parseLong(value), OTHER);
        }
        if (BizDemandGroupFieldEnum.STATUS.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return null;
            return BizDemandStatusEnum.getTextByCode(Integer.parseInt(value));
        }
        if (BizDemandGroupFieldEnum.PRIORITY.getGroupField().equals(groupField)) {
            if (StringUtils.isBlank(value)) return null;
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

    /**
     * 根据分组条件重新组装数据，填充 label1-label5 字段
     */
    private List<ProductDemandGroupFieldDO> populateLabelFields(
            List<ProductDemandGroupFieldDO> data,
            List<ViewsGroupQueryList> groupFields,
            Map<Long, Map<Long, String>> labelMaps) {

        // 找出所有标签类别分组字段
        List<Long> labelCategoryIds = new ArrayList<>();
        for (ViewsGroupQueryList groupField : groupFields) {
            if (groupField.getType() == 1) {
                labelCategoryIds.add(Long.valueOf(groupField.getKey()));
            }
        }

        if (labelCategoryIds.isEmpty()) {
            return data; // 没有标签类别分组，直接返回
        }

        // 按照唯一标识符对数据进行分组，避免重复
        Map<String, ProductDemandGroupFieldDO> groupedData = new LinkedHashMap<>();

        for (ProductDemandGroupFieldDO row : data) {
            // 构建唯一键，基于非标签字段
            String uniqueKey = buildUniqueKey(row);

            ProductDemandGroupFieldDO existingRow = groupedData.get(uniqueKey);
            if (existingRow == null) {
                // 为每个标签类别填充对应的 label 字段
                ProductDemandGroupFieldDO newRow = copyRow(row);

                // 清空所有 label 字段
                newRow.setLabelId1(null);
                newRow.setLabelId2(null);
                newRow.setLabelId3(null);
                newRow.setLabelId4(null);
                newRow.setLabelId5(null);

                // 根据原始 labelId 和标签类别映射，填充对应的 label 字段
                Long originalLabelId = row.getLabelId();
                if (originalLabelId != null) {
                    for (int i = 0; i < labelCategoryIds.size() && i < 5; i++) {
                        Long categoryId = labelCategoryIds.get(i);
                        Map<Long, String> categoryLabels = labelMaps.get(categoryId);

                        if (categoryLabels != null && categoryLabels.containsKey(originalLabelId)) {
                            // 这个 labelId 属于当前类别，填充到对应的 label 字段
                            setLabelField(newRow, i + 1, originalLabelId);
                        }
                    }
                }

                groupedData.put(uniqueKey, newRow);
            } else {
                // 合并标签信息
                Long originalLabelId = row.getLabelId();
                if (originalLabelId != null) {
                    for (int i = 0; i < labelCategoryIds.size() && i < 5; i++) {
                        Long categoryId = labelCategoryIds.get(i);
                        Map<Long, String> categoryLabels = labelMaps.get(categoryId);

                        if (categoryLabels != null && categoryLabels.containsKey(originalLabelId) && getLabelField(existingRow, i + 1) == null) {
                            // 设置对应的 label 字段，如果还没有设置的话
                            setLabelField(existingRow, i + 1, originalLabelId);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(groupedData.values());
    }

    private List<BizDemandGroupFieldDO> populateBizLabelFields(
            List<BizDemandGroupFieldDO> data,
            List<ViewsGroupQueryList> groupFields,
            Map<Long, Map<Long, String>> labelMaps) {

        // 找出所有标签类别分组字段
        List<Long> labelCategoryIds = new ArrayList<>();
        for (ViewsGroupQueryList groupField : groupFields) {
            if (groupField.getType() == 1) {
                labelCategoryIds.add(Long.valueOf(groupField.getKey()));
            }
        }

        if (labelCategoryIds.isEmpty()) {
            return data; // 没有标签类别分组，直接返回
        }

        // 按照唯一标识符对数据进行分组，避免重复
        Map<String, BizDemandGroupFieldDO> groupedData = new LinkedHashMap<>();

        for (BizDemandGroupFieldDO row : data) {
            // 构建唯一键，基于非标签字段
            String uniqueKey = buildBizUniqueKey(row);

            BizDemandGroupFieldDO existingRow = groupedData.get(uniqueKey);
            if (existingRow == null) {
                // 为每个标签类别填充对应的 label 字段
                BizDemandGroupFieldDO newRow = copyBizRow(row);

                // 清空所有 label 字段
                newRow.setLabelId1(null);
                newRow.setLabelId2(null);
                newRow.setLabelId3(null);
                newRow.setLabelId4(null);
                newRow.setLabelId5(null);

                // 根据原始 labelId 和标签类别映射，填充对应的 label 字段
                Long originalLabelId = row.getLabelId();
                if (originalLabelId != null) {
                    for (int i = 0; i < labelCategoryIds.size() && i < 5; i++) {
                        Long categoryId = labelCategoryIds.get(i);
                        Map<Long, String> categoryLabels = labelMaps.get(categoryId);

                        if (categoryLabels != null && categoryLabels.containsKey(originalLabelId)) {
                            // 这个 labelId 属于当前类别，填充到对应的 label 字段
                            setBizLabelField(newRow, i + 1, originalLabelId);
                        }
                    }
                }

                groupedData.put(uniqueKey, newRow);
            } else {
                // 合并标签信息
                Long originalLabelId = row.getLabelId();
                if (originalLabelId != null) {
                    for (int i = 0; i < labelCategoryIds.size() && i < 5; i++) {
                        Long categoryId = labelCategoryIds.get(i);
                        Map<Long, String> categoryLabels = labelMaps.get(categoryId);

                        if (categoryLabels != null && categoryLabels.containsKey(originalLabelId)) {
                            // 设置对应的 label 字段，如果还没有设置的话
                            if (getBizLabelField(existingRow, i + 1) == null) {
                                setBizLabelField(existingRow, i + 1, originalLabelId);
                            }
                        }
                    }
                }
            }
        }

        return new ArrayList<>(groupedData.values());
    }

    /**
     * 构建行数据的唯一键，基于非标签字段
     */
    private String buildUniqueKey(ProductDemandGroupFieldDO row) {
        String sb = row.getBizDomainId() + "|" +
                row.getProductLineId() + "|" +
                row.getType() + "|" +
                row.getStatus() + "|" +
                row.getPriority() + "|" +
                row.getExpectScheduleTime() + "|" +
                row.getOwnerId();
        return sb;
    }

    /**
     * 构建行数据的唯一键，基于非标签字段
     */
    private String buildBizUniqueKey(BizDemandGroupFieldDO row) {
        String sb = row.getBizDomainId() + "|" +
                row.getProductLineId() + "|" +
                row.getDeptId() + "|" +
                row.getStatus() + "|" +
                row.getPriority() + "|" +
                row.getTargetCustomer() + "|" +
                row.getReceiveManId();
        return sb;
    }

    /**
     * 获取指定位置的 label 字段值
     */
    private Long getLabelField(ProductDemandGroupFieldDO row, int position) {
        switch (position) {
            case 1:
                return row.getLabelId1();
            case 2:
                return row.getLabelId2();
            case 3:
                return row.getLabelId3();
            case 4:
                return row.getLabelId4();
            case 5:
                return row.getLabelId5();
            default:
                return null;
        }
    }

    /**
     * 获取指定位置的 label 字段值
     */
    private Long getBizLabelField(BizDemandGroupFieldDO row, int position) {
        switch (position) {
            case 1:
                return row.getLabelId1();
            case 2:
                return row.getLabelId2();
            case 3:
                return row.getLabelId3();
            case 4:
                return row.getLabelId4();
            case 5:
                return row.getLabelId5();
            default:
                return null;
        }
    }

    /**
     * 设置指定位置的 label 字段
     */
    private void setLabelField(ProductDemandGroupFieldDO row, int position, Long labelId) {
        switch (position) {
            case 1:
                row.setLabelId1(labelId);
                break;
            case 2:
                row.setLabelId2(labelId);
                break;
            case 3:
                row.setLabelId3(labelId);
                break;
            case 4:
                row.setLabelId4(labelId);
                break;
            case 5:
                row.setLabelId5(labelId);
                break;
        }
    }

    /**
     * 设置指定位置的 label 字段
     */
    private void setBizLabelField(BizDemandGroupFieldDO row, int position, Long labelId) {
        switch (position) {
            case 1:
                row.setLabelId1(labelId);
                break;
            case 2:
                row.setLabelId2(labelId);
                break;
            case 3:
                row.setLabelId3(labelId);
                break;
            case 4:
                row.setLabelId4(labelId);
                break;
            case 5:
                row.setLabelId5(labelId);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + position);
        }
    }

    /**
     * 复制行数据
     */
    private ProductDemandGroupFieldDO copyRow(ProductDemandGroupFieldDO row) {
        ProductDemandGroupFieldDO copy = new ProductDemandGroupFieldDO();
        copy.setBizDomainId(row.getBizDomainId());
        copy.setProductLineId(row.getProductLineId());
        copy.setLabelId(row.getLabelId());
        copy.setLabelId1(row.getLabelId1());
        copy.setLabelId2(row.getLabelId2());
        copy.setLabelId3(row.getLabelId3());
        copy.setLabelId4(row.getLabelId4());
        copy.setLabelId5(row.getLabelId5());
        copy.setType(row.getType());
        copy.setStatus(row.getStatus());
        copy.setPriority(row.getPriority());
        copy.setExpectScheduleTime(row.getExpectScheduleTime());
        copy.setOwnerId(row.getOwnerId());
        copy.setTotal(row.getTotal());
        return copy;
    }

    /**
     * 复制行数据
     */
    private BizDemandGroupFieldDO copyBizRow(BizDemandGroupFieldDO row) {
        BizDemandGroupFieldDO copy = new BizDemandGroupFieldDO();
        copy.setBizDomainId(row.getBizDomainId());
        copy.setProductLineId(row.getProductLineId());
        copy.setLabelId(row.getLabelId());
        copy.setLabelId1(row.getLabelId1());
        copy.setLabelId2(row.getLabelId2());
        copy.setLabelId3(row.getLabelId3());
        copy.setLabelId4(row.getLabelId4());
        copy.setLabelId5(row.getLabelId5());
        copy.setTargetCustomer(row.getTargetCustomer());
        copy.setStatus(row.getStatus());
        copy.setPriority(row.getPriority());
        copy.setDeptId(row.getDeptId());
        copy.setReceiveManId(row.getReceiveManId());
        copy.setTotal(row.getTotal());
        return copy;
    }
}
