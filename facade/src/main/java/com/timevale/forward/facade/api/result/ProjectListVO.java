package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/02/23 09:49
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点")
public class ProjectListVO extends ToString {
    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;
}
