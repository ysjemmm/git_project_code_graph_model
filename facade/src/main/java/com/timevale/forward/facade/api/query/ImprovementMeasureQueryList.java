package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @author by YangXu
 * @date 2022/03/16 16:05
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("改进事项-列表查询")
public class ImprovementMeasureQueryList extends QueryBase {

    @NotNull(message = "故障工单id不能为空")
    @ApiModelProperty("故障工单id")
    @Pattern(regexp = "^[1-9]+[0-9]*$", message = "id只能为正整数")
    private String troubleTicketId;
}
