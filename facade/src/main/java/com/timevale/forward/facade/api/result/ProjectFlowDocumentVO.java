package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@ApiModel("项目流程文档查询对象")
public class ProjectFlowDocumentVO extends ToString {

    @ApiModelProperty("项目流程id")
    private Long id;

    @ApiModelProperty("设计说明")
    private String reviewUrl;

    @ApiModelProperty("附件")
    private List<FileVO> files;

    @ApiModelProperty("创建人id")
    private String createManId;
    @ApiModelProperty("创建人")
    private String createMan;
    @ApiModelProperty("更新人id")
    private String modifyManId;
    @ApiModelProperty("更新人")
    private String modifyMan;
    @ApiModelProperty("创建时间")
    private Date createDate;
    @ApiModelProperty("更新时间")
    private Date modifyDate;

}
