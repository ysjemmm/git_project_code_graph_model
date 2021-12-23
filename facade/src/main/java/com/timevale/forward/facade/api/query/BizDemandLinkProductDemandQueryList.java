package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 17:14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求关联产品需求查询")
public class BizDemandLinkProductDemandQueryList extends QueryBase {

    @ApiModelProperty("产品需求主题")
    private String name;

    @ApiModelProperty("产品需求id")
    private Long id;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private List<Integer> priorityList;

    @ApiModelProperty("业务域id")
    private List<Long> bizDomainIdList;

    @ApiModelProperty("产品线id")
    private List<Long> productLineIdList;

    @ApiModelProperty("产品需求类型")
    private Integer type;

    @ApiModelProperty("产品需求负责人")
    private List<PersonQuery> ownerInfoList;

    @ApiModelProperty("起始时间")
    private Date createDateStart;

    @ApiModelProperty("结束时间")
    private Date createDateEnd;

    @ApiModelProperty("产品需求状态")
    private Integer status;
}
