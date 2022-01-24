package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/01/21 18:01
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-近三周计划上线项目-查询")
public class HomePageProjectOnlineLatelyQueryList extends QueryBase {

    @ApiModelProperty("用户花名")
    String userName;

    @ApiModelProperty("用户id")
    String userId;
}
