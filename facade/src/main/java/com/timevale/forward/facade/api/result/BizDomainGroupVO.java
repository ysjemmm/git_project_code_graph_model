package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author by qiyuan
 * @date 2025/08/25 16:38
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务域集")
public class BizDomainGroupVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("负责人id")
    private String ownerId;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("上架状态：0-未上架，1-已上架")
    private Integer listingStatus;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改时间")
    private Date modifyDate;

    @ApiModelProperty("业务域")
    private List<BizDomainVO> bizDomainVOS;
}
