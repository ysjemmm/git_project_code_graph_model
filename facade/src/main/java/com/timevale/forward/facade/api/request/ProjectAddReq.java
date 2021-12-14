package com.timevale.forward.facade.api.request;

import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.ProjectNodeVO;
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
@ApiModel("项目新增")
public class ProjectAddReq extends BaseReq {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2)")
    private Byte priority;

    @ApiModelProperty("产品线")
    private List<Long> productLineId;

    @ApiModelProperty("项目类型:0产品研发项目,1技术优化项目,2日常迭代")
    private Byte type;

    @ApiModelProperty("项目经理")
    private String pm;

    @ApiModelProperty("产品经理")
    private List<PersonVO> pd;

    @ApiModelProperty("团队成员")
    private List<PersonVO> teamMember;

    @ApiModelProperty("项目计划开始时间")
    private String planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private String planEndDate;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("节点")
    private ProjectNodeVO projectNodeVO;

}
