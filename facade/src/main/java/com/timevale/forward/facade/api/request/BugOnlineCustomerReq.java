package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.Collection;


/**
 * @author by YangXu
 * @date 2023/03/29 18:06
 */
@Setter
@Getter
@ApiModel("客开id查询线上bug请求")
public class BugOnlineCustomerReq extends ToString {

    @ApiModelProperty("客开id集合")
    @NotEmpty(message = "客开id不能为空")
    private Collection<Long> customerIds;
}