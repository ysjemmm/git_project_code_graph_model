package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;


/**
 * @author by YangXu
 * @date 2023/02/06 15:31
 */
@Getter
@Setter
@ApiModel("项目结项申请单详情")
public class ConclusionFormVO extends ToString {

    @ApiModelProperty("项目id")
    private Long id;

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("项目类型")
    private String kindName;

    @ApiModelProperty("项目性质")
    private String typeName;

    @ApiModelProperty("项目等级")
    private String levelName;

    @ApiModelProperty("项目状态")
    private String statusName;

    @ApiModelProperty("计划总工作量")
    private BigDecimal planWorkloadSum;

    @ApiModelProperty("工作量(计算积分)")
    private BigDecimal pointsWorkloadSum;

    @ApiModelProperty("评价列表")
    private List<ProjectEvaluateItemVO> evaluateItemVOList;

}
