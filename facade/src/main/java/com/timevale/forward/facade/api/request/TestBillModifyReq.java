package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @Date 2022/1/25 14:01
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("提测单修改")
public class TestBillModifyReq extends BaseReq {

    @ApiModelProperty(value = "项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty(value = "测试人")
    private String testMan;

    @ApiModelProperty(value = "测试人花名拼音")
    private String testManId;

    @ApiModelProperty(value = "影响范围与变更SQL")
    private String desc;

    @ApiModelProperty(value = "用例执行情况")
    private Integer progress;

    @ApiModelProperty(value = "上传附件集合")
    private List<FileAddReq> list;

    @ApiModelProperty(value = "实际提测时间")
    private Date actualDate;

    @ApiModelProperty(value = "提测通过率")
    private Integer passRate;

    @ApiModelProperty(value = "提测打回原因")
    private String reason;

    @ApiModelProperty(value = "测试用例链接")
    private String caseUrl;
}