package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectProductLineBizDomain;
import org.apache.ibatis.annotations.Param;

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
     * @return
     */
    List<ProjectProductLineBizDomain> getByProjectIds(@Param("projectIds") List<Long> projectIds);

    /**
     * 获取产品线
     *
     * @param ids id
     * @return {@link ProductLineDO }
     */
    List<ProductLineDO> selectByIds(@Param("ids") List<Long> ids);

    /**
     *
     * @param id 产品线id
     * @return
     */
    ProjectProductLineBizDomain getById(@Param("id") Long id);
}
