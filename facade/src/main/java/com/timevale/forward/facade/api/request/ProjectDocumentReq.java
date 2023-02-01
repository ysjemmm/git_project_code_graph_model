package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author xiaoyun
 * @date 2022/9/1/001 10:14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("项目文档请求")
public class ProjectDocumentReq extends BaseReq {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("projectId")
    private Long projectId;

    @ApiModelProperty("项目文档url")
    private String url;

    @ApiModelProperty("文档类型：1.产品需求文档")
    private Integer type;

    @ApiModelProperty("文件名称")
    private String docName;

    @ApiModelProperty("文档所属项目阶段:11:启动阶段;12:规划阶段;13:执行阶段;14:收尾阶段;15:运营阶段")
    private Integer stage;

    @ApiModelProperty(value = "上传附件集合")
    private List<FileAddReq> fileList;

}
