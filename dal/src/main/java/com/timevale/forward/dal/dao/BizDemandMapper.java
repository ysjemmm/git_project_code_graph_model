package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BizDemandGroupQueryCondition;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.BizDemandUpdateCondition;
import com.timevale.forward.dal.condition.ProductDemandGroupQueryCondition;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandGroupFieldDO;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.dal.entity.ProductDemandGroupFieldDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProjectDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 16:28
 */
public interface BizDemandMapper {

    /**
     * 新增单条信息
     *
     * @param bizDemandDO 业务需求DO
     * @return int
     */
    int insert(BizDemandDO bizDemandDO);

    /**
     * 更新业务需求信息
     *
     * @param bizDemandDO 业务需求DO
     * @return int
     */
    int update(BizDemandDO bizDemandDO);

    /**
     * 按需更新
     */
    void updateConditional(BizDemandUpdateCondition condition);


    /**
     * 更新业务需求信息, 仅更新可以为null的字段
     *
     * @param bdDO 业务需求DO
     */
    void updateCanNull(BizDemandDO bdDO);

    /**
     * 更新业务需求信息-完全更新
     *
     * @param bizDemandDO 业务需求DO
     * @return int
     */
    int fullUpdate(BizDemandDO bizDemandDO);

    /**
     * 选择id获取对应业务需求信息
     *
     * @param id id
     * @return 业务需求DO
     */
    BizDemandDO get(@Param("id") Long id);

    /**
     * 选择name获取对应业务需求信息
     *
     * @param name 的名字
     * @return 业务需求DO
     */
    BizDemandDO selectByName(@Param("name") String name);

    /**
     * 根据条件查询对应需求，查询列表使用
     *
     * @param bizDemandListCondition 查询条件
     * @return 列表
     */
    List<BizDemandListDO> selectList(BizDemandListCondition bizDemandListCondition);

    /**
     * 查询全部
     *
     * @return 业务需求DO
     */
    List<BizDemandDO> selectAll();

    /**
     * 根据条件查询对应需求，查询列表使用
     *
     * @param productDemandId 查询条件
     * @return 列表
     */
    List<BizDemandListDO> linkBizDemandList(@Param("productDemandId") Long productDemandId);

    /**
     * 更新业务需求
     *
     * @param ids 产品需求DO
     * @return int
     */
    int updateByIds(@Param("ids") Collection<Long> ids,
                    @Param("status") Integer status,
                    @Param("retainModifyDate") boolean retainModifyDate);

    /**
     * 更新业务需求
     *
     * @param idList      业务需求id 列表
     * @param createMan   提交人
     * @param createManId 提交人id
     */
    int updateCreateMan(@Param("idList") List<Long> idList, @Param("createMan") String createMan, @Param("createManId") String createManId);

    /**
     * 修改需求提交人
     *
     * @param idList      业务需求id 列表
     * @param submitMan   提交人
     * @param submitManId 提交人id
     */
    int updateSubmitMan(@Param("idList") List<Long> idList, @Param("submitMan") String submitMan, @Param("submitManId") String submitManId, @Param("deptId") Long deptId);


    /**
     * 修改需求接收人
     *
     * @param idList       业务需求id 列表
     * @param receiveMan   接收人
     * @param receiveManId 接收人id
     */
    int updateReceiveMan(@Param("idList") List<Long> idList, @Param("receiveMan") String receiveMan, @Param("receiveManId") String receiveManId);

    /**
     * 更新项目发布时间数据
     *
     * @param projectEndDate  项目结束日期
     * @param planReleaseDate 计划发布日期
     */
    int updateDate(@Param("id") Long id, @Param("projectEndDate") Date projectEndDate, @Param("planReleaseDate") Integer planReleaseDate);

    /**
     * 选择id获取对应业务需求信息
     *
     * @param ids id
     * @return 业务需求DO
     */
    List<BizDemandDO> getByIds(@Param("ids") Collection<Long> ids);

    /**
     * 选择id获取对应业务需求信息
     *
     * @param status status
     * @return 业务需求DO
     */
    List<BizDemandDO> selectByStatus(@Param("status") List<Integer> status);

    @Select("select count(*) from biz_demand where biz_id = #{bizId} and is_deleted = false")
    boolean bizIdExists(@Param("bizId") String bizId);

    /**
     * @param ids    id
     * @param status 状态
     * @return 业务需求DO
     */
    List<BizDemandDO> getSimpleBizDemands(@Param("ids") List<Long> ids, @Param("status") List<Integer> status, @Param("sourceId") String sourceId);

    /**
     * 根据客户id查询
     */
    List<BizDemandListDO> selectByCustomId(@Param("customId") Long customId);

    /**
     * 根据来源id列表查询业务需求列表
     */
    List<BizDemandDO> selectBySourceIds(@Param("sourceIds") Collection<String> sourceIds);

    List<BizDemandDO> getByProjectId(@Param("projectId") Long projectId);

    @Select("select p.* from project p left join project_biz_demand pbd on p.id = pbd.project_id and pbd.is_deleted = false where pbd.biz_demand_id = #{bizDemandId} and p.is_deleted = false")
    ProjectDO getByBizDemandId(@Param("bizDemandId") Long bizDemandId);

    void updateReason(@Param("id") Long id, @Param("reason") Integer reason);

    @Select("SELECT * FROM biz_demand WHERE receive_man_id=#{receiveManId} AND is_deleted =false")
    List<BizDemandDO> getByReceiveManId(@Param("receiveManId")String receiveManId);

    /**
     * 通过产品线ID获取业务需求
     *
     * @param productLineId 产品线ID
     * @return
     */
    @Select("select * from biz_demand where product_line_id=#{productLineId} AND is_deleted=false")
    List<BizDemandDO> getByProductLineId(@Param("productLineId") Long productLineId);

    @Update("UPDATE biz_demand SET product_line_id = #{productLineId} WHERE id= #{id}")
    void updateProductLineById(@Param("id")Long id, @Param("productLineId")Long productLineId);

    /**
     * 查询业务需求分组条数
     *
     * @param bizDemandGroupQueryCondition 业务需求Id列表
     * @return list
     */
    List<BizDemandGroupFieldDO> getGroupTree(BizDemandGroupQueryCondition bizDemandGroupQueryCondition);

    /**
     * 查询业务需求
     *
     * @param bizDemandGroupQueryCondition 业务需求Id列表
     * @return list
     */
    List<BizDemandListDO> getGroupList(BizDemandGroupQueryCondition bizDemandGroupQueryCondition);

    List<BizDemandGroupFieldDO> getSimpleGroupList(BizDemandGroupQueryCondition groupQueryCondition);

    Long getSimpleGroupCount(BizDemandGroupQueryCondition groupCountCondition);

    List<BizDemandListDO> simpleList(BizDemandListCondition condition);
}
