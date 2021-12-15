package com.timevale.forward.facade.api.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @Date 2021/12/14 15:20
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求查询")
public class BizDemandQueryList extends QueryBase {

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("业务需求id")
    private Long id;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private Integer priority;

    @ApiModelProperty("业务域id")
    private Long bizDomainId;

    @ApiModelProperty("产品线id")
    private Long productLineId;

    @ApiModelProperty("发布时间")
    private Date releaseDate;

    @ApiModelProperty("预期上线时间")
    private Integer planReleaseDate;

    @ApiModelProperty("需求解决状态")
    private Integer status;

    @ApiModelProperty("需求接收人")
    private String receiveMan;

    @ApiModelProperty("需求部门id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @ApiModelProperty("需求提交人")
    private String createMan;

}
