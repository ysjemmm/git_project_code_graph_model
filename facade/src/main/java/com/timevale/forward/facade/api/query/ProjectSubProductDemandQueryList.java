package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
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
@ApiModel("项目-产品需求查询")
public class ProjectSubProductDemandQueryList extends QueryBase {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("业务域")
    private List<Long> bizDomainIds;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废")
    private List<Integer> status;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    private List<Integer> priorities;

    @ApiModelProperty("负责人")
    private List<String> ownerIds;

    @ApiModelProperty("创建人")
    private List<String> createManIds;

    @ApiModelProperty("创建时间开始")
    private Date createDateStart;

    @ApiModelProperty("创建时间结束")
    private Date createDateEnd;

    @ApiModelProperty("修改时间开始")
    private Date modifyDateStart;

    @ApiModelProperty("修改时间结束")
    private Date modifyDateEnd;
}
