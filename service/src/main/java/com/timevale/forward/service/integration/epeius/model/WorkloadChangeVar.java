package com.timevale.forward.service.integration.epeius.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class WorkloadChangeVar {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 项目url
     */
    private String projectUrl;
    /**
     * 项目名称
     */
    private String projectName;
    /**
     * 项目类型
     */
    private String kindName;
    /**
     * 项目性质
     */
    private String typeName;
    /**
     * 项目等级
     */
    private String levelName;
    /**
     * 变更类型
     */
    private String changeType;
    /**
     * 计划工作量—调整前
     */
    private String planWorkloadBefore;
    /**
     * 计划工作量—调整后
     */
    private String planWorkloadAfter;
    /**
     * 计划工作量增加
     */
    private String planWorkloadAddSum;
    /**
     * 积分工作量—调整前
     */
    private String pointWorkloadBefore;
    /**
     * 积分工作量—调整后
     */
    private String pointWorkloadAfter;
    /**
     * 积分工作量增加
     */
    private String pointWorkloadAddSum;
    /**
     * 变更事由
     */
    private String changeReason;
    /**
     * pbu负责人
     */
    private String pbuPrincipal;
    /**
     * pbu负责人id
     */
    private String pbuPrincipalId;
    /**
     * srId
     */
    private String srId;
    /**
     * 发起人是否为项目负责人/产研负责人
     */
    private String isPrincipal;
    /**
     * 项目负责人/产研负责人
     */
    private List<String> principalIdList;
    /**
     * pmoid列表
     */
    private List<String> PMOIdList;
}
