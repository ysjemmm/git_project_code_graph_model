package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
public class ProjectChildListReq extends QueryBase {

    @NotNull(message = "根节点id不能为空")
    @ApiModelProperty(value = "根节点项目id", required = true)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("子项目id")
    private Long childProjectId;

    @ApiModelProperty("内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目")
    private Collection<Integer> innerTypes;

    @ApiModelProperty("项目经理")
    private Collection<String> pms;

    @ApiModelProperty("项目等级")
    private Collection<Integer> levels;

    @ApiModelProperty("项目状态")
    private Collection<Integer> status;

    @ApiModelProperty("1-子项目列表; 2-可添加为子项目列表; 3-里程碑可关联项目列表")
    private Integer searchType;

}
