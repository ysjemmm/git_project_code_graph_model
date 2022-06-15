package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点事件详情")
public class TrackEventDetailVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("中文名称")
    private String cnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("评审状态:-1已撤回,0审核中,1审核通过,2审核不通过")
    private Integer status;

    @ApiModelProperty("状态")
    private String statusName;

    @ApiModelProperty("埋点平台")
    private String platformName;

    @ApiModelProperty("埋点所属端")
    private String envName;

    @ApiModelProperty("事件拒绝原因")
    private String failReason;

    @ApiModelProperty("提交人")
    private String createMan;

    @ApiModelProperty("提交人id")
    private String createManId;

    @ApiModelProperty("修改人")
    private String modifyMan;

    @ApiModelProperty("修改人id")
    private String modifyManId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("事件属性")
    private List<TrackPropVO> trackProps;

}
