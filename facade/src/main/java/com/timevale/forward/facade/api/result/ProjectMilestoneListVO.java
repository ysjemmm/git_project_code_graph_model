package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/8
 */
@Getter
@Setter
@ApiModel("里程碑列表接口")
public class ProjectMilestoneListVO extends ToString {

    @ApiModelProperty("有效阶段列表")
    private List<Integer> validStages;

    @ApiModelProperty("里程碑列表")
    private List<ProjectMilestoneVO> list;

}
