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
@ApiModel("产品需求-客户需求查询")
public class ProductLinkCustomDemandQueryList extends QueryBase {

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("客户名称")
    private String customName;

    @ApiModelProperty("客户所在页面")
    private String page;

    @ApiModelProperty("客户需求id")
    private Long id;

    @ApiModelProperty("起始时间")
    private Date createDateStart;

    @ApiModelProperty("结束时间")
    private Date createDateEnd;

    @ApiModelProperty("需求解决状态")
    private List<Integer> status;

    @ApiModelProperty("问题类别")
    private List<Integer> causes;

    @ApiModelProperty("产品端")
    private List<Integer> productEnds;

    @ApiModelProperty("需求提交人")
    private String submitMan;

    @ApiModelProperty("项目发布时间-起始时间")
    private Date projectEndDateStart;

    @ApiModelProperty("项目发布时间-结束时间")
    private Date projectEndDateEnd;
}
