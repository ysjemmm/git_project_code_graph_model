package com.timevale.forward.service.copy;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ProductDemandGroupCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandGroupItemListDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProductDemandOwnerDO;
import com.timevale.forward.facade.api.query.CustomLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandGroupList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskLinkProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.request.ProductDemandOwnerAddReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.ProductDemandTypeEnum;
import com.timevale.forward.model.middle.ProductDemandMD;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mapper(
        imports = {
                JSON.class,
                CollUtil.class,
                Collectors.class,
                ProductDemandTypeEnum.class,
                Arrays.class,
                StringUtils.class,
                Collections.class
        }
)
public interface ProductDemandCopier {

    ProductDemandCopier INSTANCE = Mappers.getMapper(ProductDemandCopier.class);

    /**
     * 转换转换DO
     *
     * @param productDemandAddReq 对象
     * @return ProductDemandDO
     */
    @Mapping(source = "demandOwner.userName", target = "owner")
    @Mapping(source = "demandOwner.userId", target = "ownerId")
    ProductDemandDO convert(ProductDemandAddReq productDemandAddReq);

    /**
     * 转换转换DO
     *
     * @param productDemandModifyReq 对象
     * @return ProductDemandDO
     */
    @Mapping(source = "demandOwner.userName", target = "owner")
    @Mapping(source = "demandOwner.userId", target = "ownerId")
    ProductDemandDO convert(ProductDemandModifyReq productDemandModifyReq);

    /**
     * 转换转换DO
     *
     * @param projectSubProductDemandQueryList 对象
     * @return ProductDemandListCondition
     */
    ProductDemandListCondition convert(ProjectLinkProductDemandQueryList projectSubProductDemandQueryList);

    @Mapping(target = "type", expression = "java(JSON.parseArray(productDemandListDO.getType(), Integer.class))")
    ProductDemandVO convert(ProductDemandListDO productDemandListDO);

    @Mapping(target = "type", expression = "java(JSON.parseArray(productDemandGroupItemListDO.getType(), Integer.class))")
    @Mapping(source = "demandCreateDate", target = "createDate")
    @Mapping(source = "demandModifyDate", target = "modifyDate")
    @Mapping(source = "demandCreateManId", target = "createManId")
    @Mapping(source = "demandCreateMan", target = "createMan")
    @Mapping(source = "productDemandId", target = "id")
    ProductDemandVO convert(ProductDemandGroupItemListDO productDemandGroupItemListDO);

    /**
     * 转换转换DO
     *
     * @param productDemandListDO 对象
     * @return ProductDemandVO
     */
    List<ProductDemandVO> convert(List<ProductDemandListDO> productDemandListDO);

    /**
     * 转换转换DO
     *
     * @param queryList 对象
     * @return ProductDemandListCondition
     */
    @Mapping(target = "types", expression = "java(CollUtil.join(queryList.getTypes(),\"\"))")
    ProductDemandListCondition convert(ProductDemandQueryList queryList);

    /**
     * 转换转换DO
     *
     * @param productDemandDO 对象
     * @return ProductDemandDetailVO
     */
    ProductDemandDetailVO convert(ProductDemandDO productDemandDO);

    /**
     * 转换转换DO
     *
     * @param productDemandDO 对象
     * @return ProductDemandResourceDetailVO
     */
    ResourcePlanProductDemandTimeVO convertToResourceDetail(ProductDemandDO productDemandDO);

    /**
     * 转换转换VO
     *
     * @param resourceTimeVO 对象
     * @return ProductDemandDO
     */
    ProductDemandDO convertFromResourceDetail(ResourcePlanProductDemandTimeVO resourceTimeVO);

    /**
     * 转换转换DO
     *
     * @param productDemandOwnerAddReqs 对象
     * @return List<ProductDemandOwnerDO>
     */
    default List<ProductDemandOwnerDO> convertList(List<ProductDemandOwnerAddReq> productDemandOwnerAddReqs) {
        if (CollectionUtils.isEmpty(productDemandOwnerAddReqs)) {
            return Collections.emptyList();
        }
        return productDemandOwnerAddReqs.stream().map(this::convert).collect(Collectors.toList());
    }

    /**
     * 转换转换DO
     *
     * @param productDemandOwnerAddReq 对象
     * @return ProductDemandOwnerDO
     */
    ProductDemandOwnerDO convert(ProductDemandOwnerAddReq productDemandOwnerAddReq);

    /**
     * 转换转换DO
     *
     * @param productDemandOwnerDO 对象
     * @return ResourcePlanProductDemandOwnerVO
     */
    ResourcePlanProductDemandOwnerVO convert(ProductDemandOwnerDO productDemandOwnerDO);

    /**
     * 转换转换DO
     *
     * @param taskLinkProductDemandQueryList 对象
     * @return ProductDemandListCondition
     */
    ProductDemandListCondition convert(TaskLinkProductDemandQueryList taskLinkProductDemandQueryList);

    /**
     * @param productDemandDO productDemandDO
     * @return ProductDemandMD
     */
    ProductDemandMD change(ProductDemandDO productDemandDO);

    @Mapping(target = "files", ignore = true)
    ProductDemandDocumentVO convertToDocument(ProductDemandDO productDemand);

    /**
     * @param customLinkProductDemandQueryList customLinkProductDemandQueryList
     * @return ProductDemandListCondition
     */
    @Mapping(source = "productDemandId", target = "id")
    ProductDemandListCondition convert(CustomLinkProductDemandQueryList customLinkProductDemandQueryList);


    List<ProductDemandDocumentVO> convertToDocuments(List<ProductDemandDO> productDemands);

    ProductDemandListCondition convert(ProductDemandListCondition condition);

    /**
     * 转换转换DO
     *
     * @param queryList 对象
     * @return ProductDemandGroupCondition
     */
    @Mapping(target = "notInLabelIds", expression = "java(StringUtils.isNotEmpty(queryList.getNotInLabelIds()) ? Arrays.stream(queryList.getNotInLabelIds().split(\",\")).map(Long::valueOf).collect(Collectors.toList()) : null)")
    @Mapping(target = "notInOwnerIds", expression = "java(StringUtils.isNotEmpty(queryList.getNotInOwnerIds()) ? Arrays.asList(queryList.getNotInOwnerIds().split(\",\")) : null)")
    ProductDemandGroupCondition convert(ProductDemandGroupList queryList);

    /**
     * ProductDemandOwnerDO list 转换为 SimpleResourcePlanVO
     *
     * @param ownerList 对象
     * @return SimpleResourcePlanVO
     */
    default List<SimpleResourcePlanItemVO> convert(List<ProductDemandOwnerDO> ownerList, boolean generateDefault) {
        List<SimpleResourcePlanItemVO> simpleResourcePlanItems = generateDefault ? SimpleResourcePlanItemVO.defaultWithAllType()
                : new ArrayList<>();
        if (CollUtil.isEmpty(ownerList)) {
            return simpleResourcePlanItems;
        }

        Map<SimpleResourcePlanItemVO.SimpleResourceType, SimpleResourcePlanItemVO> type2Item = simpleResourcePlanItems.stream()
                .collect(Collectors.toMap(SimpleResourcePlanItemVO::getResourceType, Function.identity(), (e,r) -> e));
        ownerList.stream().collect(Collectors.groupingBy(ProductDemandOwnerDO::getResourceType)).forEach((type, owners) -> {
            SimpleResourcePlanItemVO.SimpleResourceType resourceType = SimpleResourcePlanItemVO.SimpleResourceType.fromResourceType(type);
            SimpleResourcePlanItemVO currentItem = type2Item.computeIfAbsent(resourceType, k -> SimpleResourcePlanItemVO.create(type));
            if (CollUtil.isNotEmpty(owners)) {
                currentItem.setOwners(owners.stream().map(o -> new PersonVO().setUserId(o.getOwnerId()).setUserName(o.getOwner())).collect(Collectors.toList()));
            }
        });
        return new ArrayList<>(type2Item.values());
    }
}
