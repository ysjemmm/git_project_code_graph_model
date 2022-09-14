package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求批量查询")
public class BizDemandGetReq extends BaseReq {

    @ApiModelProperty("业务需求id")
    private List<Long>ids;

    @ApiModelProperty("业务需求状态")
    private List<Integer>status;

    @ApiModelProperty("来源id(客开项目id)")
    private String sourceId;

}
