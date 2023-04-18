package com.timevale.forward.service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 配置中心配置列表
 *
 * @author jingchun
 * create on 2022/7/6
 */
@Getter
@Component
public class CommonConfig {

    @Value("${forward.baseurl}")
    private String forwardBaseUrl;

    @Value("${workflow.baseurl}")
    private String workflowBaseUrl;

    @Value("${forward.baseurl}/productManagement")
    private String productManagementUrl;

    @Value("${forward.baseurl}/productManagement/edit?type=check&id=")
    private String productManagementViewUrl;

    @Value("${forward.baseurl}/%s/edit?type=check&id=%d")
    private String commonViewUrl;

    @Value("${evalPmoGroup:557300580}")
    private String evalPmoGroup;

    /**
     * 客开需求申诉标签
     */
    @Value("${devDemandAppealLabelId:542}")
    private Long devDemandAppealLabelId;

    /**
     * 客开需求申诉通过标签
     */
    @Value("${devDemandAppealLabelId:543}")
    private Long devDemandApproveLabelId;

}
