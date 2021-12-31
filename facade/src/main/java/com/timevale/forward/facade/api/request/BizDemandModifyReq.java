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
 * @date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求修改")
public class BizDemandModifyReq extends BaseReq {
    @ApiModelProperty("业务需求id")
    @NotNull(message = "业务需求id不能为空")
    private Long id;

    @ApiModelProperty("需求主题")
    @NotBlank(message = "需求主题不能为空")
    private String name;

    @ApiModelProperty("需求部门id")
    @NotNull(message = "需求部门不能为空")
    private Long deptId;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    @NotNull(message = "必须选择优先级")
    private Integer priority;

    @ApiModelProperty("产品线id")
    @NotNull(message = "产品线不能为空")
    private Long productLineId;

    @ApiModelProperty("影响数据指标")
    private String dataIndicators;

    @ApiModelProperty("目标客户/用户/项目")
    @NotBlank(message = "目标客户/用户/项目不能为空")
    private String targetCustomer;

    @ApiModelProperty("是否共创用户")
    @NotNull(message = "共创用户不能为空")
    private Boolean createCustomer;

    @ApiModelProperty("操作系统:0 XP, 1 Win7,2 Win8，3 Win10，4中标麒麟，5银河麒麟，6麒麟V10，7中科方德，8统信UOS")
    private Integer os;

    @ApiModelProperty("处理器: 0X86/X64, 1兆芯，2飞腾，3龙芯，4鲲鹏，5申威")
    private Integer processor;

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("抄送人")
    private List<PersonAddReq> recipientInfoList;

    @ApiModelProperty("附件列表")
    private List<FileAddReq> fileList;
}
