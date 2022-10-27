package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目文档校验")
public class ProjectDocumentCheckReq extends BaseReq {
    
    @ApiModelProperty("id")
    @NotNull(message = "项目id不能为空")
    private Long id;

    @ApiModelProperty("节点")
    @NotNull(message = "项目节点不能为空")
    private List<ProjectNodeAddReq> projectNodes;

    @ApiModelProperty("项目类型:0产品研发项目,1技术优化项目,2日常迭代")
    @NotNull(message = "项目类型不能为空")
    private Integer type;
}
