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
public class TestBillVO extends ToString {
    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("项目状态:0待提交冒烟用例,1待自测,2待提测预演,3提测成功")
    private Integer status;

    @ApiModelProperty("项目状态名字")
    private String statusName;

    @ApiModelProperty("自测情况:0冒烟用例执行通过,1冒烟用例部分执行,2冒烟用例未执行")
    private Integer progress;

    @ApiModelProperty("自测情况名称")
    private String progressName;

    @ApiModelProperty("测试人")
    private String testMan;

    @ApiModelProperty("测试人花名拼音")
    private String testManId;

    @ApiModelProperty("提测次数")
    private Integer testCount;

    @ApiModelProperty("打回次数")
    private Integer returnCount;

    @ApiModelProperty("测试用例链接")
    private String caseUrl;

    @ApiModelProperty("提测失败原因")
    private String reason;

    @ApiModelProperty("提测通过率")
    private Double passRate;

    @ApiModelProperty("影响范围与变更SQL")
    private String desc;

    @ApiModelProperty("提测单主题")
    private String submitTestName;

    @ApiModelProperty("项目经理")
    private String projectManager;

    @ApiModelProperty("附件集合")
    private List<FileVO> fileVOList;

    @ApiModelProperty("实际提测时间")
    private Date actualDate;

    @ApiModelProperty("计划提测时间")
    private Date planDate;

    @ApiModelProperty("是否延期")
    private Boolean isDelay;

    @ApiModelProperty("延期天数")
    private Integer delayDay;

    @ApiModelProperty("提测人")
    private String testBillMan;

    @ApiModelProperty("提测人id")
    private String testBillManId;


}