package com.timevale.forward.facade.api.result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/14 14:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("团队人员信息")
public class TeamMemberVO extends PersonVO {
    
    @ApiModelProperty("离职:true,在职:false")
    private Boolean quited;

}
