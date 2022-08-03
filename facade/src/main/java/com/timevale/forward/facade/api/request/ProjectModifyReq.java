package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目修改")
public class ProjectModifyReq extends ProjectAddReq {
    
    @ApiModelProperty("id")
    @NotNull(message = "项目id不能为空")
    private Long id;

    @ApiModelProperty("节点")
    @NotNull(message = "项目节点不能为空")
    private List<ProjectNodeAddReq> projectNodes;

    @ApiModelProperty("节点审批流程")
    private ProjectNodeFlowAddReq projectNodeFlow;

    @ApiModelProperty("延期类型:0提测延期,1发布正式延期,2立项预期上线时间小于发布正式计划时间,-1不延期")
    @NotNull(message = "延期类型不能为空")
    private Integer delayType;

    @ApiModelProperty("立项开始时间")
    private Date pjEstablishStartDate;

    @ApiModelProperty("立项预期上线时间")
    private Date pjEstablishPublishDate;

}
