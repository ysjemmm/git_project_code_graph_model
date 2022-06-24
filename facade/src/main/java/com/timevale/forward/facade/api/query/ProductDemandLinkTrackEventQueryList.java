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
@ApiModel("产品需求-事件查询")
public class ProductDemandLinkTrackEventQueryList extends QueryBase {

    @ApiModelProperty("产品需求id")
    private Long productDemandId;

    @ApiModelProperty("中文名称")
    private String fullCnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("提交人id")
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
