package com.timevale.forward.service.config;

import lombok.Data;

import java.io.Serializable;

/**
 * @auther: yuhua
 * @date: 2025/9/15 10:39
 * @description:
 */
@Data
public class UseCaseQueryConfig implements Serializable {

    private String useCaseHost;

    private String addTestPlanModuleUrl;

    private String addTestPlanUrl;

    public String statisticsUrl;

    public String checkModuleExistUrl;

    public String queryProjectUrl;

    public String searchProjectsUrl;

    public String testplanDetailsUrl;
}
