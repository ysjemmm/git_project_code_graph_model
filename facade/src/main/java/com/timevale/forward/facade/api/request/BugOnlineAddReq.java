package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/17 14:34
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug新增")
public class BugOnlineAddReq extends BaseReq {
    @ApiModelProperty("标题")
    @NotNull(message = "标题不能为空")
    private String name;

    @ApiModelProperty("提出人:格式 花名-真名")
    @NotNull(message = "提出人不能为空")
    private String proposer;

    @ApiModelProperty("提出人花名拼音")
    @NotNull(message = "提出人花名拼音不能为空")
    private String proposerId;

    @ApiModelProperty("经办人:格式 花名-真名")
    @NotNull(message = "经办人不能为空")
    private String operator;

    @ApiModelProperty("经办人花名拼音")
    @NotNull(message = "经办人花名拼音不能为空")
    private String operatorId;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty(value = "产品线id")
    @NotNull(message = "产品线id不能为空")
    private List<Long> productLineIdList;

    @ApiModelProperty(value = "bug环境：0生产环境，1模拟环境")
    @NotNull(message = "bug环境不能为空")
    private Integer env;

    @ApiModelProperty(value = "bug所属端：0后端bug，1PC客户端，2PCweb端，3Android，4IOS，5H5")
    @NotNull(message = "bug所属端不能为空")
    private Integer belong;

    @ApiModelProperty(value = "bug优先级：0低，1中，2高，3紧急")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @ApiModelProperty("是否复现：0是，1否")
    private Integer recurrent;

    @ApiModelProperty(value = "bug原因：0需求问题，1环境配置问题，2功能错误，3兼容性问题，4数据问题，5性能问题，6安全问题，7外部原因，8开发误操作，9接口文档编写错误，10外包项目，11历史版本，12无测试参与版本，13测试环境延期未修复bug，14设计缺陷，15定制版升级改动波及，16无法重现但客户环境偶现，17无法重现但客户环境必现")
    private Integer reason;

    @ApiModelProperty("期望解决日期")
    private Date expectDate;

    @ApiModelProperty("产品线相关业务，格式是json字符串格式:{flowId->流程flow id,mainOId->实名主体 oid,templateId->模板id,appId->appid,sealId->sealid,operatorNameAccount->操作人姓名账号,loginAccount->登录账号}")
    private String business;

    @ApiModelProperty("bug详情描述")
    private String describe;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;

    @ApiModelProperty("抄送人")
    private List<PersonAddReq> recipients;
}