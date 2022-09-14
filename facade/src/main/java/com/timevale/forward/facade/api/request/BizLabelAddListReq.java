package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("标签批量新增")
public class BizLabelAddListReq extends BaseReq {

    @ApiModelProperty("标签集合")
    private List<Long> labelIdList;

    @ApiModelProperty("业务id")
    @NotNull(message = "业务id不能为空")
    private Long bizId;

    @ApiModelProperty("模块类型(10业务需求、11产品需求、12项目、13线下bug、14线上bug)")
    @NotNull(message = "模块类型不能为空")
    private Integer type;
}
