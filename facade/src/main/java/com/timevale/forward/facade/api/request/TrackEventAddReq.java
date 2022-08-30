package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点事件新增")
public class TrackEventAddReq extends BaseReq {

    @ApiModelProperty("埋点事件id")
    private Long id;

    @ApiModelProperty("中文名称")
    private String cnName;

    @ApiModelProperty("中文名全称")
    @Length(max = 100, message = "事件中文名全称字符数不能超过100")
    private String fullCnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("埋点页面id")
    @NotNull(message = "埋点页面id不能为空")
    private Long pageId;

    @ApiModelProperty("埋点元素id")
    private Long elementId;

    @ApiModelProperty("埋点平台")
    private List<Integer> platforms;

    @ApiModelProperty("埋点所属端")
    private List<Integer> envs;

    @ApiModelProperty("接口名称")
    private String apiName;

    @ApiModelProperty("触发时机")
    private String touchMoment;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;

    @ApiModelProperty("事件属性")
    @NotNull(message = "事件属性不能为空")
    private List<TrackPropAddReq> trackProps;

    @ApiModelProperty("埋点说明")
    @NotNull(message = "埋点说明不能为空")
    private String explanation;
}
