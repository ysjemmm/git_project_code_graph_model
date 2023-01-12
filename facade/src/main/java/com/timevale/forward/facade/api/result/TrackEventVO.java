package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点事件列表")
public class TrackEventVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("中文名称")
    private String cnName;

    @ApiModelProperty("中文名全称")
    private String fullCnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("评审状态:-1已撤回,0审核中,1审核通过,2审核不通过")
    private Integer status;

    @ApiModelProperty("状态")
    private String statusName;

    @ApiModelProperty("埋点平台")
    private String platform;

    @ApiModelProperty("埋点平台:0IOS、1Android、2JavaScript、3小程序、4服务端、9其他")
    private List<String> platformNames;

    @ApiModelProperty("埋点所属端")
    private String env;

    @ApiModelProperty("埋点所属端:1测试环境,2模拟环境,3生产环境")
    private List<String> envNames;

    @ApiModelProperty("触发时机")
    private String touchMoment;

    @ApiModelProperty("提交人")
    private String createMan;

    @ApiModelProperty("提交人id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;


}
