package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("客户需求新增")
public class CustomDemandAddReq extends BaseReq {

    @ApiModelProperty("产品端:0标准签PC,1支付宝小程序,2钉签H5,3钉签PC,4PC客户端,5移动端安卓,6移动端苹果,7钉签e人事,8招签宝,9标微信小程序,10实名,11签署,12填写,13用印")
    @NotNull(message = "产品端不能为空")
    private Integer productEnd;

    @ApiModelProperty("客户名称")
    @NotNull(message = "客户名称不能为空")
    private String customName;

    @ApiModelProperty("需求提交人")
    @NotNull(message = "需求提交人不能为空")
    private String submitMan;

    @ApiModelProperty("客户所在页面")
    @NotNull(message = "客户所在页面不能为空")
    private String page;

    @ApiModelProperty("所在页面链接/路径")
    private String path;

    @ApiModelProperty("联系方式")
    private String contact;

    @ApiModelProperty("问题类别:0功能缺失、1产品不可用、2界面不美观、3复杂难用、9其他")
    @NotNull(message = "问题类别不能为空")
    private String cause;

    @ApiModelProperty("描述")
    @NotNull(message = "描述不能为空")
    private String desc;

    @ApiModelProperty("附件列表")
    @Valid
    private List<FileAddReq> files;

}
