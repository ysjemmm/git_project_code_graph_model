package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BizDemandLinkProductDemandListCondition;
import com.timevale.forward.dal.condition.ProductDemandGroupQueryCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.*;
import lombok.NonNull;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface ProductDemandMapper {
    /**
     * 新增单条产品需求
     *
     * @param productDemandDO 产品需求DO
     * @return int
     */
    int insert(ProductDemandDO productDemandDO);

    /**
     * 选择id查询
     *
     * @param id id
     * @return DO
     */
    ProductDemandDO selectById(Long id);

    /**
     * 根据项目id查询
     * @param projectId 项目d
     * @return 产品需求列表
     */
    List<ProductDemandDO> selectByProjectId(Long projectId);

    /**
     * 批量查询产品需求
     *
     * @param productDemandIdList 产品需求id列表
     * @return list
     */
    List<ProductDemandDO> selectByIdList(@Param("productDemandIdList") Collection<Long> productDemandIdList);

    List<ProductDemandDO> selectList();

    /**
     * 选择通过业务需求id
     *
     * @param bizDemandId 业务需求id
     * @return {@link List<BizDemandLinkProductDemandListDO> }
     */
    List<BizDemandLinkProductDemandListDO> selectByBizDemandId(@Param("bizDemandId") Long bizDemandId);

    /**
     * 业务需求关联产品查询
     *
     * @param bizDemandLinkProductDemandListCondition 业务需求链接产品需求列表条件
     * @return list
     */
    List<BizDemandLinkProductDemandListDO> selectListOfBizDemandLink(BizDemandLinkProductDemandListCondition bizDemandLinkProductDemandListCondition);

    /**
     * 查询产品需求
     *
     * @param productDemandListCondition 产品需求Id列表
     * @return list
     */
    List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition);

    /**
     *
     * @param projectId 查询条件
     * @return 项目产品需求清单
     */
    List<ProductDemandListDO> linkProductDemandList(@Param("projectId") Long projectId, @Param("demandName") String demandName);


    /**
     * 更新单条产品需求
     *
     * @param productDemandDO 产品需求DO
     * @return int
     */
    int update(ProductDemandDO productDemandDO);

    /**
     * 更新单条产品需求状态
     *
     * @param productDemandDO 产品需求DO
     * @return int
     */
    int updateStatus(ProductDemandDO productDemandDO);

    /**
     * 批量更新产品需求状态
     * @param ids ids
     * @param demand 需求
     * @return int
     */
    int batchUpdateStatus(@NonNull @Param("ids") Collection<Long> ids, @NonNull @Param("demand") ProductDemandDO demand);

    /**
     * 更新单条产品需求资源规划
     *
     * @param productDemandDO 产品需求DO
     * @return int
     */
    int updateResourcePlan(ProductDemandDO productDemandDO);

    /**
     * 更新负责人 by id列表
     *
     * @param idList  id列表
     * @param owner   负责人
     * @param ownerId 负责人id
     */
    int updateOwner(@Param("idList") List<Long>idList, @Param("owner") String owner, @Param("ownerId") String ownerId);

    /**
     * 查询
     * @param id id
     * @return 产品需求DO
     */
    ProductDemandDO get(@Param("id") Long id);


    /**
     * 更新产品需求
     *
     * @param ids 产品需求DO
     * @return int
     */
    int updateByIds(@Param("ids") List<Long> ids,@Param("status") Integer status,@Param("retainModifyDate") boolean retainModifyDate);

    /**
     * 更新产品需求的人天
     *
     * @param demands 产品需求DO
     * @return int
     */
    int updateResourceTime(@Param("demands") List<ProductDemandDO> demands);

    /**
     * 查询
     * @param name name
     * @return 产品需求DO
     */
    ProductDemandDO getByName(@Param("name") String name);

    /**
     *
     * @param customDemandId customDemandId
     * @return 项目产品需求清单
     */
    List<ProductDemandListDO> linkProductDemandInCustomDemand(@Param("customDemandId") Long customDemandId);

    /**
     * 查询产品需求
     *
     * @param ownerId ownerId
     * @return list
     */
    List<ProductDemandDO> getByOwnerId(@Param("ownerId") String ownerId);

    /**
     * 通过产品线ID获取产品需求
     *
     * @param productLineId 产品线ID
     * @return
     */
    @Select("select * from product_demand where product_line_id=#{productLineId} AND is_deleted=false")
    List<ProductDemandDO> getByProductLineId(@Param("productLineId") Long productLineId);

    /**
     * 查询产品需求分组条数
     *
     * @param productDemandGroupCondition 产品需求Id列表
     * @return list
     */
    List<ProductDemandGroupFieldDO> getGroupTree(ProductDemandGroupQueryCondition productDemandGroupCondition);

    /**
     * 查询产品需求
     *
     * @param productDemandGroupCondition 产品需求Id列表
     * @return list
     */
    List<ProductDemandListDO> getGroupList(ProductDemandGroupQueryCondition productDemandGroupCondition);

    List<ProductDemandGroupFieldDO> getSimpleGroupList(ProductDemandGroupQueryCondition condition);

    Long getSimpleGroupCount(ProductDemandGroupQueryCondition condition);

    /**
     * 批量变更需求负责人
     * @param productDemandOwnerDoS 待变更信息
     * @return int
     */
    int batchUpsertProductDemandOwners(@Param("productDemandOwnerDoS") Collection<ProductDemandOwnerDO> productDemandOwnerDoS,
                                       @NonNull @Param("operatorId") String operatorId, @NonNull @Param("operator") String operator);

    /**
     * 批量删除需求负责人
     * @param ids 主键表ids
     * @return int
     */
    int batchDeleteProductDemandOwners(@Param("ids") Collection<Long> ids,
                                       @NonNull @Param("operatorId") String operatorId, @NonNull @Param("operator") String operator);

    /**
     * 查询需求关联的负责人
     * @param productDemandIds 产品需求ids
     * @return List<ProductDemandOwnerDO>
     */
    List<ProductDemandOwnerDO> listProductDemandOwners(@Param("productDemandIds") Collection<Long> productDemandIds);

    /**
     * 查询需求关联的负责人
     * @param productDemandGroupId 产品需求组id
     * @return List<ProductDemandOwnerDO>
     */
    List<ProductDemandOwnerDO> listProductDemandOwnersByGroupId(@Param("productDemandGroupId") Long productDemandGroupId);


    List<ProductDemandOwnerDO> listProductDemandOwnersByIds(@Param("productDemandIds") List<Long> productDemandGroupIds);

    List<ProductDemandListDO> simpleList(ProductDemandListCondition productDemandListCondition);
}
