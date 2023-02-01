package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
@ApiModel("项目树数据")
public class ProjectTreeVO extends ToString {

    @ApiModelProperty("当前节点项目id")
    private Long projectId;

    @ApiModelProperty("当前节点项目名称")
    private String projectName;

    @ApiModelProperty("子节点列表")
    private List<ProjectTreeVO> children;

}
