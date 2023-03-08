package com.timevale.forward.service.integration.epeius.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 项目结项工作流请求
 * @author by YangXu
 * @date 2023/03/08 11:20
 */
@Getter
@Setter
public class ConclusionVar {

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
}
