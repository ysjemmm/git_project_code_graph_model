package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-风险预警")
public class HomePageRiskWarningVO extends ToString {

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目计划上线时间")
    private Date planEndDate;

    @ApiModelProperty("项目节点")
    private List<HomePageProjectNodeVO> homePageProjectNodeVOList;

    @ApiModelProperty("提测")
    private List<HomePageSubmitTestVO> homePageSubmitTestVOList;

    @ApiModelProperty("任务")
    private List<HomePageTaskVO> homePageTaskVOList;
}
