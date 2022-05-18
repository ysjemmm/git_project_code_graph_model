package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug新增")
public class BugOfflineAddReq extends BaseReq {

    @ApiModelProperty("名称")
    @NotNull(message = "名称不能为空")
    private String name;

    @ApiModelProperty(value = "项目id")
    private Long projectId;

    @ApiModelProperty(value = "产品线id")
    @NotNull(message = "产品线id不能为空")
    private Long productLineId;

    @ApiModelProperty(value = "优先级:0紧急,10高,20中,30低")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @ApiModelProperty(value = "0预演bug,1测试阶段bug,2历史版本bug,3自动化脚本执行发现bug")
    @NotNull(message = "bug来源不能为空")
    private Integer source;

    @ApiModelProperty(value = "bug原因:0功能错误；1功能缺失；2改动波及；3参数校验错误；4历史遗留；5实现与需求不符；6配置错误；7环境部署；8页面格式错误；9文案提示；10UI和原型不一致；11数据问题；12需求问题；13兼容性问题；14交互体验；15交付文档错误；16优化建议；17性能问题；18安全问题；19数据库问题；20低级错误；21外部原因；22重复出现")
    @NotNull(message = "bug原因不能为空")
    private Integer reason;

    @ApiModelProperty(value = "0后端bug,1PC客户端,2PCweb端,3Android,4IOS,5H5")
    @NotNull(message = "bug所属端不能为空")
    private Integer belong;

    @ApiModelProperty(value = "0项目环境,1测试环境,2模拟环境,3生产环境")
    @NotNull(message = "bug环境不能为空")
    private Integer env;

    @ApiModelProperty(value = "0必现,1偶现")
    @NotNull(message = "复现频率不能为空")
    private Integer frequency;

    @NotNull(message = "描述不能为空")
    private String desc;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;

    @ApiModelProperty("抄送人")
    private List<PersonAddReq> recipients;

    @NotNull(message = "提出人")
    private String proposer;

    @NotNull(message = "提出人花名拼音")
    private String proposerId;

    @NotNull(message = "经办人")
    private String operator;

    @NotNull(message = "经办人花名拼音")
    private String operatorId;

    @ApiModelProperty(value = "bug平台来源")
    private String origin;
}
