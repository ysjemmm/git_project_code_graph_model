package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author jingchun
 * create on 2022/6/20
 */

@Getter
@Setter
@ApiModel("人天列表展示类")
@Accessors(chain = true)
public class ManDayListVO extends ToString {

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目创建日期")
    private Date projectCreateDate;

    @ApiModelProperty("是否项目经理")
    private boolean pm = false;

    @ApiModelProperty("人天列表")
    private List<ManDayVO> manDays;

}
