package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * @author by YangXu
 * @date 2021/12/13 16:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("埋点地图信息")
public class TrackMapVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("菜单层级:1业务域,2产品线,3模块,4埋点页面,5埋点元素")
    private Integer level;

    @ApiModelProperty("上级菜单id")
    private Long parentId;

    @ApiModelProperty("子节点")
    private List<TrackMapVO> children;

}
