package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName: FastSearchConditionVO
 * @Author: shaoye
 * @Date: 2023-08-22 14:51
 */
@ApiModel("首页-项目人员排期看板-快捷搜索条件")
@Data
@EqualsAndHashCode(callSuper = true)
public class FastSearchConditionVO extends ToString {

    @ApiModelProperty("类型：1-部门，2-成员")
    private Integer type;

    @ApiModelProperty("ID")
    private String id;

    @ApiModelProperty("显示名称")
    private String showName;

}
