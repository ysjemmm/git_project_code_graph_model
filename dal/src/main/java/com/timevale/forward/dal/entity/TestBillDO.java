package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Date 2022/1/24 16:37
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TestBillDO extends BaseDO {
    /**
     * 项目id
     */
    Long projectId;
    /**
     * 项目状态
     */
    Integer status;
    /**
     * 用例执行情况
     */
    Integer progress;
    /**
     * 测试人
     */
    String testMan;
    /**
     * 测试人花名拼音
     */
    String testManId;
    /**
     * 提测次数
     */
    Integer testCount;
    /**
     * 打回次数
     */
    Integer returnCount;
    /**
     * 测试用例链接
     */
    String caseUrl;
    /**
     * 提测失败原因
     */
    String reason;
    /**
     * 提测通过率
     */
    Integer passRate;
    /**
     * 影响范围与变更SQL
     */
    String desc;


}