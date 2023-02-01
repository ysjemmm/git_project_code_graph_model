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
@ApiModel("项目文档查询对象")
public class ProjectDocumentVO extends ToString {

    @ApiModelProperty("项目流程id")
    private Long id;

    @ApiModelProperty("文档类型：1.产品需求文档;11.立项申请报告;12.项目方案报告;13审计计划;14.项目复盘报告;15:运营计划;16:其他")
    private Integer type;

    @ApiModelProperty("url")
    private String url;

    @ApiModelProperty("文件名称")
    private String docName;

    @ApiModelProperty("文档所属项目阶段:11:启动阶段;12:规划阶段;13:执行阶段;14:收尾阶段;15:运营阶段")
    private Integer stage;

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
