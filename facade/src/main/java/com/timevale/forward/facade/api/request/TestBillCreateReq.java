package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/02/15 18:35
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("提测单创建")
public class TestBillCreateReq extends BaseReq{

    @ApiModelProperty("项目id")
    private Long projectId;
}
