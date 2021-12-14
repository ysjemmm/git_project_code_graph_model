package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("文件")
public class FileVO extends ToString {

    @ApiModelProperty("文件id")
    private String fileId;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件下载地址")
    private String downloadUrl;

}
