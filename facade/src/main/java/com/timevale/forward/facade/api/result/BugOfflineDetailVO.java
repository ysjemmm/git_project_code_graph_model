package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug详情")
public class BugOfflineDetailVO extends ToString {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目状态")
    private Integer projectStatus;

    @ApiModelProperty("产品线")
    private ProductLineVO productLineVO;

    @ApiModelProperty(value = "状态:0bug打开、1待修复、2待验收、3待确认、4延期修复、5完成、6关闭")
    private Integer status;

    @ApiModelProperty(value = "bug状态")
    private String statusName;

    @ApiModelProperty(value = "优先级:0紧急,10高,20中,30低")
    private Integer priority;

    @ApiModelProperty(value = "优先级")
    private String priorityName;

    @ApiModelProperty(value = "bug来源:0预演bug,1测试阶段bug,2历史版本bug,3自动化脚本执行发现bug")
    private Integer source;

    @ApiModelProperty(value = "bug来源")
    private String sourceName;

    @ApiModelProperty("bug原因：1功能错误，2功能缺失，3改动波及，4参数校验错误，5历史遗留，6实现与需求不符，7配置错误，8环境部署，9页面格式错误，10文案提示，11UI和原型不一致，12数据问题，13需求问题，14兼容性问题，15交互体验，16交付文档错误，17优化建议，18性能问题，19安全问题，20数据库问题，21低级错误，22外部原因，23重复出现，24合并代码冲突")
    private Integer reason;

    @ApiModelProperty(value = "bug原因名字")
    private String reasonName;

    @ApiModelProperty(value = "bug所属端:0后端bug,1PC客户端,2PCweb端,3Android,4IOS,5H5")
    private Integer belong;

    @ApiModelProperty(value = "bug所属端")
    private String belongName;

    @ApiModelProperty(value = "bug环境:0项目环境,1测试环境,2模拟环境,3生产环境")
    private Integer env;

    @ApiModelProperty(value = "bug环境")
    private String envName;

    @ApiModelProperty(value = "复现频率:0必现,1偶现")
    private Integer frequency;

    @ApiModelProperty(value = "复现频率")
    private String frequencyName;

    @ApiModelProperty(value = "详情描述")
    private String desc;

    @ApiModelProperty(value = "延期修复原因")
    private String delayHandleReason;

    @ApiModelProperty(value = "不用修复原因:0被否定,1重复提交,2无法再次复现,3前端缓存,4产品需求调整,10无")
    private Integer unhandleReason;

    @ApiModelProperty(value = "不用修复原因")
    private String unhandleReasonName;

    @ApiModelProperty(value = "完成后重新打开次数")
    private Integer openCount;

    @ApiModelProperty(value = "打回次数")
    private Integer returnCount;

    @ApiModelProperty(value = "bug提出人名字")
    private String proposer;

    @ApiModelProperty(value = "bug提出人id")
    private String proposerId;

    @ApiModelProperty("附件集合")
    private List<FileVO> files;

    @ApiModelProperty("经办人")
    private String operator;

    @ApiModelProperty("经办人id")
    private String operatorId;

    @ApiModelProperty("所属业务域")
    private BizDomainVO bizDomainVO;

    @ApiModelProperty("抄送人")
    private List<PersonVO> recipientInfoList;

    @ApiModelProperty("评论")
    private List<CommentVO> commentVOList;

    @ApiModelProperty("提交时间")
    private Date submitDate;

    @ApiModelProperty("最后更新时间")
    private Date lastModifyDate;

    @ApiModelProperty("bug产生原因")
    private String cause;

    @ApiModelProperty("解决方案")
    private String solvePlan;

    @ApiModelProperty("预计解决完成日期")
    private Date expectSolveDate;
}
