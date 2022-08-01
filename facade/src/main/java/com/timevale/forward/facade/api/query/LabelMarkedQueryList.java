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
@ApiModel("查询被标记的业务")
public class LabelMarkedQueryList extends ToString {

    @ApiModelProperty("选中为标签时填写,标签id")
    private Long labelId;

    @ApiModelProperty("选中为标签类别时填写,类别id")
    private Long labelCategoryId;

}
