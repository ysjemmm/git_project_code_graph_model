package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Date 2022/3/17 16:34
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug 不用修复")
public class BugOnlineNoRepairReq extends BaseReq {
    @ApiModelProperty("线上bug id")
    @NotNull(message = "线上bug id不能为空")
    private Long id;

    @ApiModelProperty("驳回原因：0客户操作错误，1客户对业务理解错误，2产品不支持，3客户的回调地址错误，4客户对接版本错误，5配置套餐没有费用，6重复提交，7支行大额行号未配置，8实施传参错误，9网络波动，10客户自身缺陷，11实施给客户项目的配置错误，12实施对业务理解错误，13操作人录入错误，14需求变更，15历史数据未订正，16文档与实际不符，17长时间未反馈，18问题描述不清，19当前版本不支持，20可以升级版本解决，21报告人提供信息不全无法排查，22产品配置错误，23客户侧环境问题，24技术咨询")
    @NotNull(message = "驳回原因不能为空")
    private Integer dismissCause;
}