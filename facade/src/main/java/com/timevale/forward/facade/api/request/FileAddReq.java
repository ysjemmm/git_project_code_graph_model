package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: xingyun
 * @create: 2021-12-13 17:44
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("文件新增")
public class FileAddReq extends BaseReq {

    @ApiModelProperty(value = "文件名称")
    private String name;

    @ApiModelProperty(value = "文件id")
    private String fileId;

}
