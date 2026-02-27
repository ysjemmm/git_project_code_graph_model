package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author qiyuan
 * @date 2025-08-25 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务域集新增")
public class BizDomainGroupAddReq extends BaseReq {

    @ApiModelProperty("名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @ApiModelProperty("负责人")
    @NotBlank(message = "负责人不能为空")
    private String owner;

    @ApiModelProperty("负责人id")
    @NotBlank(message = "负责人id不能为空")
    private String ownerId;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("业务域id")
    @NotNull(message = "业务域id不能为空")
    private List<Long> bizDomainIds;


    @ApiModelProperty("估算模式：resource=传统资源规划, storyPoint=故事点")
    private String estimationMode;


}
