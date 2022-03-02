package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    private PersonVO receiveManInfo;

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("附件")
    private List<FileVO> fileList;

    @ApiModelProperty("抄送人")
    private List<PersonVO> recipientInfoList;

    @ApiModelProperty("项目发布时间")
    private Date endDate;

    @ApiModelProperty("驳回理由")
    private Integer reason;

    @ApiModelProperty("驳回理由文本")
    private String reasonText;
}
