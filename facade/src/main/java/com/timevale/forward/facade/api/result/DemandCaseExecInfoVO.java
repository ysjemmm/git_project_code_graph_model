package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @auther: yuhua
 * @date: 2025/10/29 17:06
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("需求用例执行信息")
public class DemandCaseExecInfoVO extends ToString {

    // 0:非生产，1:生产
    @ApiModelProperty("是否生产")
    private Integer turnType;

    @ApiModelProperty("用例通过数")
    private Integer passNum;

    @ApiModelProperty("用例失败数")
    private Integer failNum;

    @ApiModelProperty("用例阻塞数")
    private Integer blockNum;

    @ApiModelProperty("总用例数")
    private Integer totalNum;

    @ApiModelProperty("用例通过率")
    private String passRate = "0.0%";
}
