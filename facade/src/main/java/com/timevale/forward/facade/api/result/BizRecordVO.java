package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;


/**
 * 业务状态变更日志
 *
 * @author by YangXu
 * @date 2023/05/15 15:18
 */
@Getter
@Setter
@ApiModel("业务状态变更日志")
public class BizRecordVO extends ToString {
    @ApiModelProperty("业务状态")
    private String status;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("bug状态操作人")
    private List<BizStatusOperatorVO> statusOperatorVOList;
}