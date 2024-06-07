package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BizDemandLinkProductDemandListCondition;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.facade.api.query.BizDemandLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.query.ProductDemandLinkBizDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandAddReq;
import com.timevale.forward.facade.api.request.BizDemandAgreeReq;
import com.timevale.forward.facade.api.request.BizDemandModifyReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.BizDemandMD;
import com.timevale.forward.model.to.PdLineDomainTO;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/15 10:56
 */
@Mapper(
        imports = {
                DateUtil.class,
                YesOrNoEnum.class,
                PriorityEnum.class,
                BizDemandReasonEnum.class,
                BizDemandStatusEnum.class,
                PlanReleaseDateEnum.class,
                CustomerDevTypeEnum.class,
                PlanReleaseDateEnum.class,
        }
)
public interface BizDemandCopier {
    BizDemandCopier INSTANCE = Mappers.getMapper(BizDemandCopier.class);

    /**
     * 查询条件转换
     *
     * @param query 业务需求查询条件
     * @return 查询条件
     */
    @Mapping(target = "createDateStart", expression = "java(DateUtil.getStartOfDay(query.getCreateDateStart()))")
    @Mapping(target = "createDateEnd", expression = "java(DateUtil.getEndOfDay(query.getCreateDateEnd()))")
    @Mapping(target = "projectEndDateStart", expression = "java(DateUtil.getStartOfDay(query.getProjectEndDateStart()))")
    @Mapping(target = "projectEndDateEnd", expression = "java(DateUtil.getEndOfDay(query.getProjectEndDateEnd()))")
    BizDemandListCondition convert(BizDemandQueryList query);


    /**
     * 请求添加转换为DO
     *
     * @param bizDemandAddReq 业务需求添加
     * @return 业务需求DO
     */
    BizDemandDO convert(BizDemandAddReq bizDemandAddReq);


    /**
     * 请求修改转换为DO
     *
     * @param bizDemandModifyReq 业务需求修改要求的事情
     * @return DO
     */
    BizDemandDO convert(BizDemandModifyReq bizDemandModifyReq);

    /**
     * 业务需求DO转换为VO
     *
     * @param bizDemandDO 业务需求DO
     * @return 业务需求详细VO
     */
    @Mapping(target = "reasonText", expression = "java(BizDemandReasonEnum.getTextByCode(bizDemandDO.getReason()))")
    @Mapping(target = "statusText", expression = "java(BizDemandStatusEnum.getTextByCode(bizDemandDO.getStatus()))")
    @Mapping(target = "priorityText", expression = "java(PriorityEnum.getTextChineseByCode(bizDemandDO.getPriority()))")
    @Mapping(target = "customerDevDemandText", expression = "java(YesOrNoEnum.getTextByCode(bizDemandDO.getCustomerDevDemand()))")
    @Mapping(target = "planReleaseDateText", expression = "java(PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate()))")
    @Mapping(target = "customerDevTypeText", expression = "java(CustomerDevTypeEnum.getTextByCode(bizDemandDO.getCustomerDevType()))")
    BizDemandDetailVO convert(BizDemandDO bizDemandDO);

    /**
     * 业务需求DO转换为VO
     *
     * @param bizDemandDO 业务需求DO
     * @return 业务需求详细VO
     */
    @Mapping(target = "productLineId", source = "bizDemandDO.productLineId")
    @Mapping(target = "priorityText", expression = "java(PriorityEnum.getTextChineseByCode(bizDemandDO.getPriority()))")
    @Mapping(target = "planReleaseDateText", expression = "java(PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate()))")
    BizDemandSimpleVO do2svo(BizDemandDO bizDemandDO, PdLineDomainTO pdLineDomainTO);

    /**
     * 业务需求DO转换为VO
     *
     * @param bizDemandDO 业务需求DO
     * @return 业务需求详细VO
     */
    @Mapping(target = "statusText", expression = "java(BizDemandStatusEnum.getTextByCode(bizDemandDO.getStatus()))")
    @Mapping(target = "priorityText", expression = "java(PriorityEnum.getTextByCode(bizDemandDO.getPriority()))")
    BizDemandVO transfer(BizDemandDO bizDemandDO);

    /**
     * 业务需求DO转换为VO
     *
     * @param bizDemandDO 业务需求DO
     * @return 业务需求详细VO
     */
    List<BizDemandVO> transfer(List<BizDemandDO> bizDemandDO);


    /**
     * 转换
     *
     * @param listDO 业务需求列表DO
     * @return VO
     */
    @Mapping(target = "statusText", expression = "java(BizDemandStatusEnum.getTextByCode(listDO.getStatus()))")
    @Mapping(target = "priorityText", expression = "java(PriorityEnum.getTextChineseByCode(listDO.getPriority()))")
    @Mapping(target = "customerDevDemand", expression = "java(YesOrNoEnum.getTextByCode(listDO.getCustomerDevDemand()))")
    @Mapping(target = "affectCustomerOrder", expression = "java(YesOrNoEnum.getTextByCode(listDO.getAffectCustomerOrder()))")
    @Mapping(target = "planReleaseDateText", expression = "java(PlanReleaseDateEnum.getTextByCode(listDO.getPlanReleaseDate()))")
    BizDemandVO convert(BizDemandListDO listDO);

    /**
     * DO批量转换为VO
     *
     * @param list 列表
     * @return 业务需求列表
     */
    List<BizDemandVO> convert(List<BizDemandListDO> list);

    /**
     * 业务需求查询关联产品条件转换
     *
     * @param list 列表
     * @return Condition
     */
    @Mapping(source = "ownerInfoList", target = "ownerIdList", qualifiedByName = "getInfoId")
    BizDemandLinkProductDemandListCondition convert(BizDemandLinkProductDemandQueryList list);


    /**
     * 转换
     *
     * @param list 分页数据
     * @return VO
     */
    PageQueryResult<BizDemandVO> convert(PageQueryResult<BizDemandDO> list);


    /**
     * 变换
     *
     * @param bizDemandLinkProductDemandListDO 业务需求链接产品需求列表DO
     * @return VO
     */
    @Mapping(source = "owner", target = "ownerInfo.userName")
    @Mapping(source = "ownerId", target = "ownerInfo.userId")
    BizDemandLinkProductDemandVO transform(BizDemandLinkProductDemandListDO bizDemandLinkProductDemandListDO);

    /**
     * 转换
     *
     * @param list 列表
     * @return list
     */
    List<BizDemandLinkProductDemandVO> transform(List<BizDemandLinkProductDemandListDO> list);

    /**
     * 转换
     *
     * @param list 分页数据
     * @return VO
     */
    PageQueryResult<BizDemandLinkProductDemandVO> transform(PageQueryResult<BizDemandLinkProductDemandListDO> list);


    /**
     * 业务需求查询关联产品条件转换
     *
     * @param demandQueryList 列表
     * @return Condition
     */
    BizDemandListCondition convert(ProductDemandLinkBizDemandQueryList demandQueryList);


    /**
     * 变换
     *
     * @param bizDemandDO 业务需求 DO
     * @return {@code BizDemandMD}
     */
    BizDemandMD transform(BizDemandDO bizDemandDO);

    /**
     * 转换
     *
     * @param bizDemandDO 业务需求DO
     * @return {@link BizDemandStatusVO}
     */
    BizDemandStatusVO change(BizDemandDO bizDemandDO);

    @Mapping(source = "bizDemandId", target = "id")
    BizDemandDO req2do(BizDemandAgreeReq req);


    BizDemandListCondition convert(BizDemandListCondition condition);
    /**
     * 信息id
     *
     * @param list 列表
     * @return list
     */
    @Named("getInfoId")
    default List<String> getInfoId(List<PersonQuery> list){
        if(list == null){
            return null;
        }
        return list.stream().map(PersonQuery::getUserId).collect(Collectors.toList());
    }

}
