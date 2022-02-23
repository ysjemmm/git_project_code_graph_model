package com.timevale.forward.dal.entity;

import com.timevale.forward.dal.annotation.FieldCompare;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOfflineDO extends BaseDO {
    /**
     * 名称
     */
    @FieldCompare(fieldName = "bug标题")
    private String name;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 优先级不能为空
     */
    @FieldCompare(fieldName = "bug优先级",enumMapping ="{0:\"紧急\",10:\"高\",20:\"中\",30:\"低\"}")
    private Integer priority;

    /**
     * bug来源不能为空
     */
    @FieldCompare(fieldName = "bug来源",enumMapping ="{0:\"预演bug\",1:\"测试阶段bug\",2:\"历史版本bug\",3:\"自动化脚本执行发现bug\"}")
    private Integer source;

    /**
     * bug原因不能为空
     */
    @FieldCompare(fieldName = "bug原因")
    private String reason;
    /**
     * bug所属端
     */
    @FieldCompare(fieldName = "bug所属端",enumMapping ="{0:\"后端bug\",1:\"PC客户端\",2:\"PCweb端\",3:\"Android\",4:\"IOS\",5:\"H5\"}")
    private Integer belong;
    /**
     * bug环境
     */
    @FieldCompare(fieldName = "bug环境",enumMapping ="{0:\"项目环境\",1:\"测试环境\",2:\"模拟环境\",3:\"生产环境\"}")
    private Integer env;

    /**
     * 复现频率
     */
    @FieldCompare(fieldName = "bug复现频率",enumMapping ="{0:\"必现\",1:\"偶现\"}")
    private Integer frequency;

    /**
     * 描述
     */
    @FieldCompare(fieldName = "bug详情描述")
    private String desc;

    /**
     * 经办人
     */
    @FieldCompare(fieldName = "bug经办人")
    private String operator;

    /**
     * 延期修复原因
     * */
    private String delayHandleReason;

    /**
     * 不用修复原因:0被否定,1重复提交,2无法再次复现,3前端缓存,4产品需求调整,10无
     * */
    private String unHandleReason;

    /**
     * 经办人花名拼音
     */
    private String operatorId;

}
