package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-风险预警")
public class HomeRiskWarningVO extends ToString {

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目节点")
    private List<HomeProjectNodeVO>homeProjectNodeVO;

    @ApiModelProperty("提测单id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long testBillId;

    @ApiModelProperty("提测单名称")
    private String testBillName;

    @ApiModelProperty("任务")
    private List<HomeTaskVO>homeTaskVO;


}
