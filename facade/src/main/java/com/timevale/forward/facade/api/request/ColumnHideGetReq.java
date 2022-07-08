package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2022/06/24 09:52
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("列隐藏-查询")
public class ColumnHideGetReq extends BaseReq{

    @ApiModelProperty("所属模块:0 业务需求，10 产品需求， 20 项目管理， 30 任务管理， 40 线下bug， 50 线上bug， 60 故障单")
    @NotNull(message = "所属模块不能为空")
    private Integer model;

    @ApiModelProperty("tab：0 全部， 10 我的， 20 我接收的， 30 抄送我的， 40 我下属的， 50 我团队的， 60 我团结提交的， 70 我团队接收的， 80 我部门的")
    @NotNull(message = "tab类型不能为空")
    private Integer tabType;
}
