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

    @ApiModelProperty("需求提交人id列表")
    private List<String> submitManIdList;

    @ApiModelProperty("需求接收人id列表")
    private List<String> receiveManIdList;

    @ApiModelProperty("需求部门id")
    private List<Long> deptIdList;

    @ApiModelProperty("需求id")
    private Long productDemandId;

    @ApiModelProperty("是否包含标签")
    private Boolean containLabel = true;

    @ApiModelProperty("选中为标签时填写,标签id")
    private List<Long> labelIds;

    @ApiModelProperty("选中为标签类别时填写,类别id")
    private List<Long> labelCategoryIds;

}
