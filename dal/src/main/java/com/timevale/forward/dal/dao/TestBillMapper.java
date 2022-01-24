package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.SubmitTestDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Date 2022/1/21 14:34
 * @Author 望轩
 */
@Mapper
public interface TestBillMapper {

    /**
     * 提交提测单
     *
     * @param projectId   项目id
     * @param testMan     测试人
     * @param testManId   测试人花名拼音
     * @param createMan   创建人
     * @param createManId 创建人花名拼音
     */
    void submitTestBill(@Param("projectId") Long projectId, @Param("testMan") String testMan
            , @Param("testManId") String testManId, @Param("createMan") String createMan
            , @Param("createManId") String createManId);

    /**
     * 提交冒烟用例
     *
     * @param projectId   项目id
     * @param caseUrl     冒烟用例地址
     * @param createMan   创建人
     * @param createManId 创建人花名拼音
     */
    void submitSmokeTesting(@Param("projectId") Long projectId, @Param("caseUrl") String caseUrl
            , @Param("createMan") String createMan, @Param("createManId") String createManId);

    /**
     * 修改测试人
     *
     * @param projectId   项目id
     * @param testMan     测试人
     * @param testManId   测试人花名拼音
     * @param modifyMan   修改人
     * @param modifyManId 修改人花名拼音
     * @return boolean 修改结果
     */
    boolean modifyTestMan(@Param("projectId") Long projectId, @Param("testMan") String testMan
            , @Param("testManId") String testManId, @Param("modifyMan") String modifyMan
            , @Param("modifyManId") String modifyManId);

    /**
     * 自测通过
     *
     * @param projectId   项目id
     * @param desc        影响范围与变更SQL
     * @param progress    用例执行情况:0冒烟用例执行通过,1冒烟用例部分执行,2冒烟用例未执行
     * @param modifyMan   修改人
     * @param modifyManId 修改人花名拼音
     */
    void selfTestPass(@Param("projectId") Long projectId, @Param("desc") String desc, @Param("progress") Integer progress
            , @Param("modifyMan") String modifyMan, @Param("modifyManId") String modifyManId);

    /**
     * 提测通过
     *
     * @param projectId   项目id
     * @param passRate    提测通过率
     * @param modifyMan   修改人
     * @param modifyManId 修改人花名拼音
     */
    void submitTestPass(@Param("projectId") Long projectId, @Param("passRate") Integer passRate,
                        @Param("modifyMan") String modifyMan, @Param("modifyManId") String modifyManId);

    /**
     * 提测打回
     *
     * @param projectId   项目id
     * @param reason      提测原因
     * @param modifyMan   修改人
     * @param modifyManId 修改人花名拼音
     */
    void submitTestBack(@Param("projectId") Long projectId, @Param("reason") String reason
            , @Param("modifyMan") String modifyMan, @Param("modifyManId") String modifyManId);


    /**
     * 查看某个项目是否已经有提测单了
     *
     * @param projectId 项目id
     * @return boolean 返回结果
     */
    SubmitTestDO selectByProjectId(@Param("projectId") Long projectId);
}


































