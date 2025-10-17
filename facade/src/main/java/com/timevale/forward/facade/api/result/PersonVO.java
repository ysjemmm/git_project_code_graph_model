package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author by YangXu
 * @date 2021/12/14 14:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@ApiModel("人员信息")
public class PersonVO extends ToString {
    
    @ApiModelProperty("人员名字")
    private String userName;

    @ApiModelProperty("人员id")
    private String userId;
}
