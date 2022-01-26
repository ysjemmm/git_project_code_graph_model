package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

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
    @JSONField(name = "id")
    private String projectId;

    /**
     * 项目名称
     */
    @JSONField(name = "name")
    private String projectName;

    /**
     * 开始规划
     */
    @JSONField(name = "ksgh")
    private String startPlan;

    /**
     * 需求内审
     */
    @JSONField(name = "xqns")
    private String demandInternalAudit;

    /**
     * 需求串讲
     */
    @JSONField(name = "xqcj")
    private String demandConstrue;

    /**
     * 技术详设评审
     */
    @JSONField(name = "jsxsps")
    private String technicalDetailReview;

    /**
     * 开发开始
     */
    @JSONField(name = "kskf")
    private String developStart;

    /**
     * 提测
     */
    @JSONField(name = "tc")
    private String submitTest;

    /**
     * 编写测试用例
     */
    @JSONField(name = "bxcsyl")
    private String writeTestCases;

    /**
     * 用例评审
     */
    @JSONField(name = "ylps")
    private String useCaseReview;

    /**
     * 测试开始
     */
    @JSONField(name = "csks")
    private String testStart;

    /**
     * 发布模拟
     */
    @JSONField(name = "fbmn")
    private String publishSimulate;
}
