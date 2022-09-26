package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/13 16:38
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务域")
public class BizDomainVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("负责人id")
    private String ownerId;

    @ApiModelProperty("是否废弃")
    private Boolean isDeleted;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改时间")
    private Date modifyDate;
}
