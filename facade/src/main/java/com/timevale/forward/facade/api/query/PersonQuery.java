package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/20 09:38
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("人员查询")
public class PersonQuery extends ToString {

    @ApiModelProperty("别名")
    String userName;

    @ApiModelProperty("用户id")
    String userId;
}
