package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jingchun
 * create on 2022/6/20
 */

@Getter
@Setter
@ApiModel("人天列表展示类")
public class ManDayListVO extends ToString {

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("人天列表")
    private List<ManDayVO> manDays;

}
