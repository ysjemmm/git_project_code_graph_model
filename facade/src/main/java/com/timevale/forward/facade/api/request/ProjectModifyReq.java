package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 20:17
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目修改")
public class ProjectModifyReq extends ProjectAddReq {
    
    @ApiModelProperty("id")
    private Long id;
}
