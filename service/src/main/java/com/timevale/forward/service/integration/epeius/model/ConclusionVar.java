package com.timevale.forward.service.integration.epeius.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 项目结项工作流请求
 * @author by YangXu
 * @date 2023/03/08 11:20
 */
@Getter
@Setter
public class ConclusionVar {
    private Long projectId;

    private String projectName;

    private String projectUrl;

    private String kindName;

    private String typeName;

    private String levelName;

    private String statusName;

    private List<String> principalIdList;

    private String srId;

    private List<String> PMOIdList;

    private String planWorkloadSum;

    private String pointsWorkloadSum;

    private String isPrincipal;

    private List<ProjectEvaluateVar> evaluateList;

    private List<ProjectEvaluateVar> reviewerEvaluateList;

    // 低代码有bug，以下为临时写法
    private String selfProgress;
    private String selfQuality;
    private String selfTarget;
    private String principalProgress;
    private String principalQuality;
    private String principalTarget;
    private String pmoProgress;
    private String pmoQuality;
    private String pmoTarget;

    private BigDecimal selfProgressScore;
    private BigDecimal selfQualityScore;
    private BigDecimal selfTargetScore;
    private BigDecimal principalProgressScore;
    private BigDecimal principalQualityScore;
    private BigDecimal principalTargetScore;
    private BigDecimal pmoProgressScore;
    private BigDecimal pmoQualityScore;
    private BigDecimal pmoTargetScore;

    private String selfProgressScoreDesc;
    private String selfQualityScoreDesc;
    private String selfTargetScoreDesc;
    private String principalProgressScoreDesc;
    private String principalQualityScoreDesc;
    private String principalTargetScoreDesc;
    private String pmoProgressScoreDesc;
    private String pmoQualityScoreDesc;
    private String pmoTargetScoreDesc;
}
