package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.*;
import lombok.Data;

/**
 * @author by YangXu
 * @date 2022/02/25 16:16
 */
@Data
public class BugOfflineMD extends BaseMD{
    /**
     * 名称
     */
    @FieldCompare(fieldName = "bug标题")
    private String name;

    /**
     * 经办人
     */
    @FieldCompare(fieldName = "经办人")
    private String operator;

    /**
     * bug提出人
     */
    @FieldCompare(fieldName = "bug提出人")
    private String proposer;

    /**
     * bug优先级: 0紧急,10高,20中,30低
     */
    @FieldCompare(fieldName = "bug优先级", enumClass = BugPriorityEnum.class)
    private Integer priority;

    /**
     * bug环境
     */
    @FieldCompare(fieldName = "bug环境", enumClass = BugEnvEnum.class)
    private Integer env;

    /**
     * bug原因不能为空
     */
    @FieldCompare(fieldName = "bug原因", enumClass = BugReasonEnum.class)
    private Integer reason;

    /**
     * bug来源不能为空
     */
    @FieldCompare(fieldName = "bug来源", enumClass = BugSourceEnum.class)
    private Integer source;

    /**
     * bug所属端
     */
    @FieldCompare(fieldName = "bug所属端", enumClass = BugBelongEnum.class)
    private Integer belong;

    /**
     * 复现频率
     */
    @FieldCompare(fieldName = "bug复现频率", enumClass = BugFrequencyEnum.class)
    private Integer frequency;

    /**
     * 描述
     */
    @FieldCompare(fieldName = "bug详情描述")
    private String desc;


    /**
     * 延期修复原因
     */
    @FieldCompare(fieldName = "延期修复原因")
    private String delayHandleReason;

    /**
     * 不用修复原因
     */
    @FieldCompare(fieldName = "不用修复原因", enumClass = BugUnHandleReasonEnum.class)
    private Integer unhandleReason;

}
