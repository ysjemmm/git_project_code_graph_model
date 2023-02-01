package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
public class ProjectChildListReq extends QueryBase {

    @ApiModelProperty("根节点项目id")
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("子项目id")
    private Long childProjectId;

    @ApiModelProperty("内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目")
    private Integer innerType;

    @ApiModelProperty("项目经理")
    private String pm;

    @ApiModelProperty("项目等级")
    private Integer level;

    @ApiModelProperty("项目状态")
    private Collection<Integer> status;

}
