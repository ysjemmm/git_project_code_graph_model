package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

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
    private Long projectId;
    /**
     * 项目状态
     */
    private Integer status;
    /**
     * 用例执行情况
     */
    private Integer progress;
    /**
     * 测试人
     */
    private String testMan;
    /**
     * 测试人花名拼音
     */
    private String testManId;
    /**
     * 提测次数
     */
    private Integer testCount;
    /**
     * 打回次数
     */
    private Integer returnCount;
    /**
     * 测试用例链接
     */
    private String caseUrl;
    /**
     * 全量用例链接
     */
    private String allCaseUrl;
    /**
     * 提测失败原因
     */
    private String reason;
    /**
     * 提测通过率
     */
    private Double passRate;
    /**
     * 影响范围与变更SQL
     */
    private String desc;

    /**
     * 逾期天数
     */
    private Integer delayDay;

    /**
     * 文档创建时间
     */
    private Date docCreateDate;
    /**
     * 文档修改时间
     */
    private Date docModifyDate;

    /**
     * 文档创建人id
     */
    private String docCreateManId;

    /**
     * 文档创建人
     */
    private String docCreateMan;

    /**
     * 文档修改人id
     */
    private String docModifyManId;

    /**
     * 文档修改人
     */
    private String docModifyMan;


}