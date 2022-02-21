package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @ApiModelProperty("项目名称")
    private String projectId;

    @ApiModelProperty("项目id")
    private String projectName;

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

    @ApiModelProperty(value = "bug原因:0功能错误；1功能缺失；2改动波及；3参数校验错误；4历史遗留；5实现与需求不符；6配置错误；7环境部署；8页面格式错误；9文案提示；10UI和原型不一致；11数据问题；12需求问题；13兼容性问题；14交互体验；15交付文档错误；16优化建议；17性能问题；18安全问题；19数据库问题；20低级错误；21外部原因；22重复出现")
    private Integer reason;

    @ApiModelProperty(value = "优先级")
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

    @ApiModelProperty(value = "描述")
    private String desc;

    @ApiModelProperty(value = "延期修复原因")
    private String delayHandleReason;

    @ApiModelProperty(value = "不用修复原因:0被否定,1重复提交,2无法再次复现,3前端缓存,4产品需求调整,10无")
    private Integer unhandleReason;

    @ApiModelProperty(value = "不用修复原因")
    private String unhandleReasonName;

    @ApiModelProperty(value = "打开次数")
    private Integer openCount;

    @ApiModelProperty(value = "打回次数")
    private Integer returnCount;

    @ApiModelProperty(value = "bug提出人")
    private List<PersonVO> createMans;

    @ApiModelProperty("文件信息")
    private List<FileVO> files;


}
