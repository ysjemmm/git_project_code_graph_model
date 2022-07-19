package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("文件")
public class FileVO extends ToString {

    @ApiModelProperty("附件所属id")
    private Long attacheId;

    @ApiModelProperty("文件id")
    private String fileId;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件下载地址")
    private String downloadUrl;

    @ApiModelProperty(value = "文件类型:-1:产品线图片,1产品需求,2业务需求,3任务,4提测单-冒烟用例,5提测单-自测通过,6线下bug,7线上bug,8故障单,9详设评审,10埋点事件")
    private String fileType;

    @ApiModelProperty(value = "文件Key")
    private String fileKey;

}
