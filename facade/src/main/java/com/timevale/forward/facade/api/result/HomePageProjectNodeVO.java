package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-风险预警-项目节点")
public class HomePageProjectNodeVO extends ToString {

    @ApiModelProperty("名称")
    private String nodeName;

    @ApiModelProperty("逾期类型")
    private String overdueType;

    @ApiModelProperty("逾期时间")
    private String overdueDay;

    @ApiModelProperty("提测单名称")
    private String testBillName;

}
