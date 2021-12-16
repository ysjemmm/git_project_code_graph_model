package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 14:07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求详细行信息")
public class BizDemandDetailVO extends BizDemandVO {

    @ApiModelProperty("需求部门id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @ApiModelProperty("影响数据指标")
    private String dataIndicators;

    @ApiModelProperty("目标客户/用户/项目")
    private String targetCustomer;

    @ApiModelProperty("是否共创用户")
    private Boolean createCustomer;

    @ApiModelProperty("接收人")
    private String receiveMan;

    @ApiModelProperty("操作系统:0 XP, 1 Win7,2 Win8，3 Win10，4中标麒麟，5银河麒麟，6麒麟V10，7中科方德，8统信UOS")
    private Byte os;

    @ApiModelProperty("处理器: 0X86/X64, 1兆芯，2飞腾，3龙芯，4鲲鹏，5申威")
    private Byte processor;

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("附件")
    private List<FileVO> files;

    @ApiModelProperty("抄送人")
    private List<PersonVO> recipients;

    @ApiModelProperty("项目发布时间")
    private Date endDate;
}
