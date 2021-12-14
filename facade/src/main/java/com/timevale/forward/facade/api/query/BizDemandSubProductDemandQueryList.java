package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @Date 2021/12/14 17:14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求查询")
public class BizDemandSubProductDemandQueryList extends QueryBase {

    @ApiModelProperty("产品需求主题")
    private String name;

    @ApiModelProperty("产品需求id")
    private Long id;

    @ApiModelProperty("优先级")
    private Integer priority;

    @ApiModelProperty("业务域")
    private Long bizDomainId;

    @ApiModelProperty("产品线")
    private Long productLineId;

    @ApiModelProperty("产品需求类型")
    private Integer type;

    @ApiModelProperty("所属项目")
    private Long toProject;

    @ApiModelProperty("产品需求负责人")
    private String owner;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("产品需求状态")
    private Integer status;
}
