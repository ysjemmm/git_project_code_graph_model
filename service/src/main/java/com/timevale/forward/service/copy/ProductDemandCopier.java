package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskLinkProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.model.middle.ProductDemandMD;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
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
     * @param productDemandQueryList 对象
     * @return ProductDemandListCondition
     */
    ProductDemandListCondition convert(ProductDemandQueryList productDemandQueryList);

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
     * @param taskLinkProductDemandQueryList 对象
     * @return ProductDemandListCondition
     */
    ProductDemandListCondition convert(TaskLinkProductDemandQueryList taskLinkProductDemandQueryList);

    /**
     *
     * @param productDemandDO productDemandDO
     * @return ProductDemandMD
     */
    ProductDemandMD change(ProductDemandDO productDemandDO);

    @Mapping(target = "files", ignore = true)
    ProductDemandDocumentVO convertToDocument(ProductDemandDO productDemand);
    /**
     *
     * @param customLinkProductDemandQueryList customLinkProductDemandQueryList
     * @return ProductDemandListCondition
     */
    @Mapping(source = "productDemandId", target = "id")
    ProductDemandListCondition convert(CustomLinkProductDemandQueryList customLinkProductDemandQueryList);


    List<ProductDemandDocumentVO> convertToDocuments(List<ProductDemandDO> productDemands);
}
