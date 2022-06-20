package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/01/04 13:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求状态信息")
public class BizDemandStatusVO extends ToString {

    @ApiModelProperty("需求解决状态:0待评估，10已接收，20已列入项目，30项目进行中，40已完成上线，-10被驳回，-20已作废")
    private Integer status;

    @ApiModelProperty("需求解决状态名称")
    private String statusText;

    @ApiModelProperty("项目发布时间（新）")
    private Date projectEndDate;

    @ApiModelProperty("预计上线时间 12月份")
    private Integer planReleaseDate;

    @ApiModelProperty("预计上线时间名称")
    private String planReleaseDateText;

}
