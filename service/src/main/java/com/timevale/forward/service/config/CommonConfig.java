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

    @Value("${forward.baseurl}/productManagement")
    private String productManagementUrl;

    @Value("${forward.baseurl}/productManagement/edit?type=check&id=")
    private String productManagementViewUrl;

}
