package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/1/24 16:33
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("提测单")
public class SubmitTestVO extends ToString {
    @ApiModelProperty("项目id")
    Long projectId;

    @ApiModelProperty("项目状态")
    Integer status;

    @ApiModelProperty("用例执行情况")
    Integer progress;

    @ApiModelProperty("测试人")
    String testMan;

    @ApiModelProperty("测试人花名拼音")
    String testManId;

    @ApiModelProperty("提测次数")
    Integer testCount;

    @ApiModelProperty("打回次数")
    Integer returnCount;

    @ApiModelProperty("测试用例链接")
    String caseUrl;

    @ApiModelProperty("提测失败原因")
    String reason;

    @ApiModelProperty("提测通过率")
    Integer passRate;

    @ApiModelProperty("影响范围与变更SQL")
    String desc;

    @ApiModelProperty("提测单主题")
    String submitTestName;

    @ApiModelProperty("项目经理")
    String projectManager;

    @ApiModelProperty("附件集合")
    List<FileVO> fileVOList;

    @ApiModelProperty("实际提测时间")
    Date actualDate;


}