package com.timevale.forward.service.integration.epeius.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
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

    private String principalId;

    private String otnPrincipalId;

    private String srId;

    private List<String> PMOIds;

    private String planWorkloadSum;

    private String pointsWorkloadSum;

    private String isPrincipal;

    private String containPd;

    private Collection<ProjectEvaluateVar> evaluates;

    private Collection<ProjectEvaluateVar> reviewerEvaluates;

    private Collection<ProjectMemberEvaluateVar> memberEvaluates;

    private Collection<String> pdSuperiorIds;

    private String pmId;
}
