package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


/**
 * @author by YangXu
 * @date 2023/03/29 18:13
 */
@Getter
@Setter
@ApiModel("线上bug客开查询数据")
public class BugOnlineSimpleVO extends ToString {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("bug标题")
    private String name;

    @ApiModelProperty("产品线")
    private List<String> productLineNameList;

    @ApiModelProperty("业务域")
    private List<String> bizDomainNameList;

    @ApiModelProperty("经办人")
    private String operator;
}