package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.TestBillDO;
import org.apache.ibatis.annotations.Param;

/**
 * @Date 2022/1/21 14:34
 * @Author 望轩
 */
public interface TestBillMapper {

    /**
     * 提交提测单
     *
     * @param testBillDO 提测单信息
     */
    void submitTestBill(@Param("testBillDO") TestBillDO testBillDO);

    /**
     * 提交冒烟用例
     *
     * @param testBillDO 提测单修改信息
     */
    void submitSmokeTesting(@Param("testBillDO") TestBillDO testBillDO);

    /**
     * 修改测试人
     *
     * @param testBillDO 提测单修改信息
     * @return boolean 修改结果
     */
    boolean modifyTestMan(@Param("testBillDO") TestBillDO testBillDO);

    /**
     * 自测通过
     *
     * @param testBillDO 提测单修改信息
     */
    void selfTestPass(@Param("testBillDO") TestBillDO testBillDO);

    /**
     * 提测通过
     *
     * @param testBillDO 提测单修改信息
     */
    void submitTestPass(@Param("testBillDO") TestBillDO testBillDO);

    /**
     * 提测打回
     *
     * @param testBillDO 提测单修改信息
     */
    void submitTestBack(@Param("testBillDO") TestBillDO testBillDO);


    /**
     * 查看某个项目是否已经有提测单了
     *
     * @param projectId 项目id
     * @return boolean 返回结果
     */
    TestBillDO selectByProjectId(@Param("projectId") Long projectId);
}


































