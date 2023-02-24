package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2023/02/24 17:24
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("发布平台应用分页查询")
public class DevopsAppQueryList extends QueryBase {

    @ApiModelProperty("主体id")
    private Long mainId;

}