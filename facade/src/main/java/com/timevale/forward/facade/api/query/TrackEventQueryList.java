package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点事件列表查询")
public class TrackEventQueryList extends QueryBase {

    @ApiModelProperty("埋点地图id")
    private Long trackMapId;

    @ApiModelProperty("菜单层级")
    private Integer level;

    @ApiModelProperty("中文名称")
    private String cnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("状态")
    private List<Integer>status;

    @ApiModelProperty("提交人")
    private List<String> createManIds;

    @ApiModelProperty("创建时间")
    private Date createDateStart;

    @ApiModelProperty("创建时间")
    private Date createDateEnd;

    @ApiModelProperty("修改时间开始")
    private Date modifyDateStart;

    @ApiModelProperty("修改时间结束")
    private Date modifyDateEnd;

}
