package com.timevale.forward.service.copy;

import com.timevale.forward.dal.condition.BizDemandLinkProductDemandListCondition;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.facade.api.query.BizDemandLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.request.BizDemandAddReq;
import com.timevale.forward.facade.api.request.BizDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandLinkProductDemandVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.assertj.core.util.Lists;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/15 10:56
 */
@Mapper
public interface BizDemandCopier {
    BizDemandCopier INSTANCE = Mappers.getMapper(BizDemandCopier.class);

    /**
     * 查询条件转换
     *
     * @param bizDemandQueryList 业务需求查询条件
     * @return 查询条件
     */
    @Mapping(source = "createManInfoList", target = "createManIdList", qualifiedByName = "getInfoId")
    @Mapping(source = "receiveManInfoList", target = "receiveManIdList", qualifiedByName = "getInfoId")
    BizDemandListCondition convert(BizDemandQueryList bizDemandQueryList);


    /**
     * 请求添加转换为DO
     *
     * @param bizDemandAddReq 业务需求添加
     * @return 业务需求DO
     */
    @Mapping(source = "receiveManInfo.userName", target = "receiveMan")
    @Mapping(source = "receiveManInfo.userId", target = "receiveManId")
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
    @Mapping(source = "receiveMan",target = "receiveManInfo.userName")
    @Mapping(source = "receiveManId",target = "receiveManInfo.userId")
    BizDemandDetailVO convert(BizDemandDO bizDemandDO);


    /**
     * 转换
     *
     * @param bizDemandListDO 业务需求列表DO
     * @return VO
     */
    @Mapping(source = "receiveMan", target = "receiveManInfo.userName")
    @Mapping(source = "receiveManId", target = "receiveManInfo.userId")
    @Mapping(source = "createMan", target = "createManInfo.userName")
    @Mapping(source = "createManId", target = "createManInfo.userId")
    BizDemandVO convert(BizDemandListDO bizDemandListDO);

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
    PageQueryResult<BizDemandVO> convert(PageQueryResult<BizDemandVO> list);


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
    PageQueryResult<BizDemandLinkProductDemandVO> transform(PageQueryResult<BizDemandLinkProductDemandVO> list);



    /**
     * 信息id
     *
     * @param list 列表
     * @return list
     */
    @Named("getInfoId")
    default List<String> getInfoId(List<PersonQuery> list){
        return list.stream().map(PersonQuery::getUserId).collect(Collectors.toList());
    }

}
