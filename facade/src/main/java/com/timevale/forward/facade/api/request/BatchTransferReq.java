package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/04/21 14:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("批量转交请求")
public class BatchTransferReq extends BaseReq{

    @ApiModelProperty("所属模块:0 业务需求，10 产品需求， 20 项目管理， 30 任务管理， 40 线下bug， 50 线上bug， 60 故障单")
    @NotNull(message = "所属模块不能为空")
    private Integer model;

    @ApiModelProperty("需要转交的id列表")
    @NotNull(message = "转交列表不能为空")
    private List<Long> mainIdList;

    @ApiModelProperty("接收人")
    @NotBlank(message = "接收人不能为空")
    String receiveMan;

    @ApiModelProperty("接收人id")
    @NotBlank(message = "接收人id不能为空")
    String receiveManId;
}
