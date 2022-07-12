package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("客户需求新增")
public class CustomDemandAddReq extends BaseReq {

    @ApiModelProperty("产品端")
    @NotBlank(message = "产品端不能为空")
    private String productEnd;

    @ApiModelProperty("客户名称")
    @NotBlank(message = "客户名称不能为空")
    private String customName;

    @ApiModelProperty("客户所在页面")
    @NotBlank(message = "客户所在页面不能为空")
    private String page;

    @ApiModelProperty("所在页面链接/路径")
    private String path;

    @ApiModelProperty("联系方式")
    private String contact;

    @ApiModelProperty("问题类别")
    @NotBlank(message = "问题类别不能为空")
    private String cause;

    @ApiModelProperty("描述")
    @NotBlank(message = "描述不能为空")
    private String desc;

    @ApiModelProperty("附件列表")
    private List<FileAddReq> fileList;

}
