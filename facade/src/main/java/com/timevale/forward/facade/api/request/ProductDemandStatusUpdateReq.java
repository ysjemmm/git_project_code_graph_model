package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

/**
 * @auther: yuhua
 * @date: 2025/10/29 11:28
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求状态更新")
public class ProductDemandStatusUpdateReq extends BaseReq {
    @ApiModelProperty("产品需求id，与ids二选一")
    private Long id;

    @ApiModelProperty("产品需求状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @ApiModelProperty("待批量更新的产品需求id，与id二选一")
    private Set<Long> ids;

    @ApiModelProperty("业务域集ID")
    private Long bizDomainGroupId;

    @ApiModelProperty("上线时间（状态更新为完成上线时必填，只能设置一次）")
    private Date onlineTime;
}
