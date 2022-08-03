package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目日期修改")
public class ProjectDateModifyReq extends BaseReq {
    
    @ApiModelProperty("id")
    @NotNull(message = "项目id不能为空")
    private Long id;

    @ApiModelProperty("立项开始时间")
    private Date pjEstablishStartDate;

    @ApiModelProperty("立项预期上线时间")
    private Date pjEstablishPublishDate;

}
