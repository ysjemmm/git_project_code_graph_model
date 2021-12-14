package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author by YangXu
 * @Date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求新增")
@AllArgsConstructor
@NoArgsConstructor
public class BizDemandAddReq extends BizDemandModifyReq {

    @ApiModelProperty("接收人")
    private String receiveMan;

}
