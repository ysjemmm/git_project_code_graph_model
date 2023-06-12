package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;

/**
 * @author by YangXu
 * @date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug获取优先级请求")
public class BugOnlinePriorityGetReq extends ToString {
    @ApiModelProperty("客户等级: 10-S;20-A;30-B;40-C")
    private String customerGrade;

    @ApiModelProperty("客户数: 1- 单客户; 2- 2家或2家以上客户")
    private Integer customerCount;

    @ApiModelProperty("bug类别: 0-空, 1-功能问题; 2-性能问题; 3-兼容性问题; 4-用户体验问题; 5-安全问题")
    private Integer category;

    @ApiModelProperty("bug环境: 0-生产环境; 1-模拟环境")
    private Integer env;

    @ApiModelProperty("是否必现: 0-是，1-否")
    private Integer recurrent;

    @ApiModelProperty("用户数: 1- 1~2个; 2- 3个或3个以上")
    private Integer userCount;

    @ApiModelProperty("问题发生时长: 1-24小时以内; 2-24~72小时; 3-72小时以上")
    private Integer problemOccurredTime;

    @ApiModelProperty("产品线IDs")
    private Collection<Long> productLineIds;
}
