package com.timevale.forward.facade.api.query;

import com.timevale.forward.facade.api.request.BaseReq;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("文件查询")
public class FileQueryList extends BaseReq {

    @ApiModelProperty(value = "文件所属id")
    @NotNull(message = "文件所属id不能为空")
    private Long attachId;

    @ApiModelProperty(value = "文件类型:1产品需求,2业务需求,3任务,4提测单-冒烟用例,5提测单-自测通过,6线下bug,7线上bug,8故障单,9详设评审,10埋点事件")
    @NotNull(message = "文件类型不能为空")
    private Integer type;
}
