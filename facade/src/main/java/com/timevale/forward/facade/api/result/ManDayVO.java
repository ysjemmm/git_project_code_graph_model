package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@Accessors(chain = true)
@ApiModel("人天展示类")
public class ManDayVO extends ToString {

    @ApiModelProperty("人天id")
    private Long id;

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目成员id")
    private String memberId;

    @ApiModelProperty("项目成员名")
    private String memberName;

    @ApiModelProperty("实际人天")
    private BigDecimal actualManDay;

    @ApiModelProperty("周开始日期")
    private Date weekStartDate;

    @ApiModelProperty("周结束日期")
    private Date weekEndDate;

    @ApiModelProperty("周期时间范围")
    private String weekDateRange;

    @ApiModelProperty("是否可编辑")
    private boolean editable = false;

    @ApiModelProperty("是否项目经理")
    private boolean pm = false;

    @ApiModelProperty("项目经理id")
    private String pmId;

    @ApiModelProperty("项目经理名称")
    private String pmName;

    @ApiModelProperty("审计人天")
    private BigDecimal auditManDay;

    @ApiModelProperty("审计状态：0审核通过，10已驳回，20审核中")
    private Integer auditStatus;

    @ApiModelProperty("驳回理由")
    private String rejectReason;

}
