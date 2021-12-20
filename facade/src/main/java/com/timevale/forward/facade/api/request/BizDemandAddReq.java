package com.timevale.forward.facade.api.request;

import com.timevale.forward.facade.api.result.PersonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求新增")
public class BizDemandAddReq extends BizDemandModifyReq {

    @ApiModelProperty("接收人")
    private PersonAddReq receiveManInfo;
}
