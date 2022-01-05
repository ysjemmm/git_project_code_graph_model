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
 * @date 2021/12/14 15:20
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求-业务需求查询")
public class ProductDemandLinkBizDemandQueryList extends QueryBase {

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("业务需求id")
    private Long id;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private List<Integer> priorityList;

    @ApiModelProperty("业务域id")
    private List<Long> bizDomainIdList;

    @ApiModelProperty("产品线id")
    private List<Long> productLineIdList;

    @ApiModelProperty("起始时间")
    private Date createDateStart;

    @ApiModelProperty("结束时间")
    private Date createDateEnd;

    @ApiModelProperty("需求解决状态")
    private List<Integer> statusList;

    @ApiModelProperty("需求提交人")
    private List<PersonQuery> createManInfoList;

    @ApiModelProperty("需求接收人")
    private List<PersonQuery> receiveManInfoList;

    @ApiModelProperty("需求部门id")
    private List<Long> deptIdList;

    @ApiModelProperty("需求id")
    private Long productDemandId;

}
