package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;


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
}
