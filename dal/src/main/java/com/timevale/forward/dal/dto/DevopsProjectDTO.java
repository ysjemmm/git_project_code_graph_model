package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


/**
 * @author by YangXu
 * @date 2023/02/28 10:44
 */
@Getter
@Setter
public class DevopsProjectDTO {

    @JSONField(name = "name")
    private String devopsProjectName;

    @JSONField(name = "code")
    private String devopsProjectSign;

    @JSONField(name = "apps")
    private List<DevopsAppDTO> appList;
}