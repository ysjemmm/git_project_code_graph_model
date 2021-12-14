package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目列表")
public class ProjectVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2)")
    private Byte priority;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("产品线")
    private String productLine;

    @ApiModelProperty("项目类型:0产品研发项目,1技术优化项目,2日常迭代")
    private Byte type;

    @ApiModelProperty("项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,50已暂停,60已作废")
    private Byte status;

    @ApiModelProperty("项目经理")
    private String pm;

    @ApiModelProperty("产品经理")
    private String pd;

    @ApiModelProperty("项目计划开始时间")
    private String planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private String planEndDate;

    @ApiModelProperty("项目实际开始时间")
    private String actualStartDate;

    @ApiModelProperty("项目实际结束时间")
    private String actualEndDate;

    @ApiModelProperty("是否删除:0:否,1:是")
    private Byte isDeleted;

    @ApiModelProperty("创建人id")
    private String createManId;
    
    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改人id")
    private String modifyManId;

    @ApiModelProperty("修改人")
    private String modifyMan;

    @ApiModelProperty("修改时间")
    private Date modifyDate;
}
