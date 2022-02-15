package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/01/25 15:52
 */
@Data
public class HomePageProjectBoardDTO {

    /**
     * 用户id
     */
    @JSONField(name = "user_id")
    private String userId;

    /**
     * 用户id
     */
    @JSONField(name = "user_name")
    private String userName;

    /**
     * 项目id
     */
    @JSONField(name = "project_id")
    private String projectId;

    /**
     * 项目名称
     */
    @JSONField(name = "project_name")
    private String projectName;

    /**
     * 开始规划
     */
    @JSONField(name = "ksgh")
    private Date startPlan;

    /**
     * 需求内审
     */
    @JSONField(name = "xqns")
    private Date demandInternalAudit;

    /**
     * 需求串讲
     */
    @JSONField(name = "xqcj")
    private Date demandConstrue;

    /**
     * 技术详设评审
     */
    @JSONField(name = "jsxsps")
    private Date technicalDetailReview;

    /**
     * 开发开始
     */
    @JSONField(name = "kskf")
    private Date developStart;

    /**
     * 提测
     */
    @JSONField(name = "tc")
    private Date submitTest;

    /**
     * 编写测试用例
     */
    @JSONField(name = "bxcsyl")
    private Date writeTestCases;

    /**
     * 用例评审
     */
    @JSONField(name = "ylps")
    private Date useCaseReview;

    /**
     * 测试开始
     */
    @JSONField(name = "csks")
    private Date testStart;

    /**
     * 发布模拟
     */
    @JSONField(name = "fbmn")
    private Date publishSimulate;

    /**
     * 发布正式
     */
    @JSONField(name = "fbzs")
    private Date publishOfficial;

    /**
     * 项目上线日期
     */
    @JSONField(name = "plan_end_date")
    private Date planEndDate;

}
