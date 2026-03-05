package com.timevale.egress.gateway.common.enums;

import com.timevale.egress.gateway.common.constants.GatewayConstants;
import lombok.Getter;

@Getter
public enum MetadataRpcEnum {
    /**
     * 查询专属云项目元数据
     */
    QUERY_PROJECT_METAS(GatewayConstants.SAAS_INTEGRATION_SERVICE, "/queryDedicatedProjectMetas/input"),
    /**
     * 查询专属云项目接口列表
     */
    QUERY_PROJECT_URLS(GatewayConstants.SAAS_INTEGRATION_SERVICE, "/queryDedicatedProjectUrls/input"),

    /**
     * 查询专属云证书列表
     */
    GET_SSL_CERTS(GatewayConstants.SAAS_INTEGRATION_SERVICE, "/getSslCerts"),
    ;

    private final String serviceName;
    private final String url;

    MetadataRpcEnum(String serviceName, String url) {
        this.serviceName = serviceName;
        this.url = url;
    }

}