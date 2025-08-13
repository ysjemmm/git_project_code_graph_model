package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/8/13 15:09
 * @description: 需求分组节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@ApiModel("需求分组节点")
public class DemandGroupNodeVO extends ToString {

    @ApiModelProperty("字段")
    private String field;

    @ApiModelProperty("字段值")
    private String fieldValue;

    @ApiModelProperty("名称")
    private String label;

    @ApiModelProperty("子节点")
    private List<DemandGroupNodeVO> children;

    @ApiModelProperty("记录数")
    private Long total;
}
