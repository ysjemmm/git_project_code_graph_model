package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProductLineCondition;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectProductLineBizDomain;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface ProductLineMapper {

    /**
     * 获取产品线列表
     *
     * @return 列表
     */
    List<ProductLineDO> selectAllProductLine();

    /**
     * 获取产品线
     *
     * @param id id
     * @return {@link ProductLineDO }
     */
    ProductLineDO selectById(@Param("id") Long id);

    /**
     *
     * @param projectI 项目id
     * @return 列表
     */
    List<ProductLineDO> get(@Param("projectId") Long projectI);

    /**
     *
     * @param projectIds 项目id
     * @return 列表
     */
    List<ProjectProductLineBizDomain> getByProjectIds(@Param("projectIds") List<Long> projectIds);

    /**
     * 获取产品线
     *
     * @param ids id
     * @return {@link ProductLineDO }
     */
    List<ProductLineDO> getByIds(@Param("ids") Collection<Long> ids);

    /**
     *
     * @param bizDomainIds bizDomainIds
     * @return ProjectProductLineBizDomain
     */
    List<ProjectProductLineBizDomain> getPlineAndBizDomain(@Param("bizDomainIds") List<Long> bizDomainIds);

    /**
     *
     * @param bizDomainIds bizDomainIds
     * @return ProjectProductLineBizDomain
     */
    List<ProjectProductLineBizDomain> getPlineAndBizDomainList(@Param("bizDomainIds") List<Long> bizDomainIds, @Param("productLineIds") List<Long> productLineIds);

    /**
     *
     * @param bizDomainId bizDomainId
     * @return 列表
     */
    List<ProductLineDO> getBizDomainId(@Param("bizDomainId") Long bizDomainId);

    /**
     *
     * @param bizDomainName bizDomainName
     * @return 列表
     */
    List<ProductLineDO> getByBizDomainName(@Param("bizDomainName") List<String> bizDomainName);

    /**
     *
     * @param bizDomainIds 业务域id
     * @return 列表
     */
    List<Long> getByBizDomainIds(@Param("bizDomainIds") Collection<Long> bizDomainIds);

    /**
     *
     * @param productLineDO productLineDO
     * @return int
     */
    int insert(ProductLineDO productLineDO);

    /**
     *
     * @param productLineDO productLineDO
     * @return int
     */
    int update(ProductLineDO productLineDO);

    /**
     * 获取产品线
     *
     * @param condition condition
     * @return {@link ProductLineDO }
     */
    List<ProductLineDO> selectByCondition(ProductLineCondition condition);

    /**
     * 根据名称获取产品线
     */
    @Select("select * from product_line where `name` = #{name} and is_deleted = false")
    ProductLineDO selectByName(@Param("name") String name);

    /**
     * 根据名称集合获取产品线
     */
    List<ProductLineDO> selectByProductLineNames(@Param("names") List<String> names);

    /**
     * 更新产品线等级
     *
     * @param id               id
     * @param productLineLevel 产品线级
     */
    void updateProductLineLevel(@Param("id")Long id, @Param("productLineLevel")Integer productLineLevel);
}
