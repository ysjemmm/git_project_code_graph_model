package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("评论查询")
public class CommentQueryList extends ToString {
    
    @ApiModelProperty("主体id")
    private Long toId;

    @ApiModelProperty("主体类型:0:项目,1产品需求,2业务需求,3任务,4线下bug,5线上bug,6故障单,7客户需求")
    private Integer type;

}
