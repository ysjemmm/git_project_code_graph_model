package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("文件新增")
public class FileAddReq extends BaseReq {

    @ApiModelProperty(value = "文件名称")
    @NotNull(message = "文件名称不能为空")
    private String fileName;

    @ApiModelProperty(value = "文件id")
    @NotNull(message = "文件id不能为空")
    private String fileId;

    @ApiModelProperty(value = "文件类型")
    @NotNull(message = "文件类型不能为空")
    private String fileType;

}
