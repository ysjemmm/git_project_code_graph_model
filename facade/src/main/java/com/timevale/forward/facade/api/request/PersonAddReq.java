package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @Date 2021/12/15 14:42
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("添加相关人员")
public class PersonAddReq extends BaseReq {

    @ApiModelProperty("人员姓名")
    private String userName;

    @ApiModelProperty("人员id")
    private String userId;

    @ApiModelProperty("人员类型:0项目-产品经理，1项目-项目成员，20产品需求-抄送人，30业务需求-抄送人")
    private Integer type;
}
