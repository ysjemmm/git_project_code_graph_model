package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;


/**
 * @author by YangXu
 * @date 2023/02/28 10:45
 */
@Getter
@Setter
public class DevopsAppDTO {

    @JSONField(name = "name")
    private String appName;

    @JSONField(name = "typeName")
    private String appType;

    @JSONField(name = "projectBranch")
    private String appBranch;

    @JSONField(name = "k8sDomainName")
    private String appDomain;
}