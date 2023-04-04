package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.*;
import lombok.Data;

import java.util.Date;

/**
 * @Date 2022/3/22 11:08
 * @Author 望轩
 */
@Data
public class BugOnlineMD extends BaseMD{
    /**
     * bug标题
     */
    @FieldCompare(fieldName = "bug标题")
    private String name;

    /**
     * 经办人
     */
    @FieldCompare(fieldName = "经办人")
    private String operator;

    /**
     * 提出人
     */
    @FieldCompare(fieldName = "提出人")
    private String proposer;

    /**
     * 客户名称
     */
    @FieldCompare(fieldName = "客户名称")
    private String customerName;

    /**
     * bug环境：0生产环境，1模拟环境
     */
    @FieldCompare(fieldName = "bug环境", enumClass = BugOnlineEnvEnum.class)
    private Integer env;

    /**
     * bug所属端：0后端bug，1PC客户端，2PCweb端，3Android，4IOS，5H5
     */
    @FieldCompare(fieldName = "bug所属端", enumClass = BugOnlineBeloneEnum.class)
    private Integer belong;

    /**
     * bug优先级：0低，1中，2高，3紧急
     */
    @FieldCompare(fieldName = "bug优先级", enumClass = BugOnlinePriorityEnum.class)
    private Integer priority;

    /**
     * 是否复现：0是，1否
     */
    @FieldCompare(fieldName = "能否复现", enumClass = BugOnlineRecurrentEnum.class)
    private Integer recurrent;

    /**
     * 期望解决日期
     */
    @FieldCompare(fieldName = "期望解决日期")
    private Date expectDate;

    /**
     * 产品线相关业务，格式是json字符串格式
     */
    private String business;

    /**
     * bug详情描述
     */
    @FieldCompare(fieldName = "bug详情描述")
    private String describe;

    /**
     * 问题原因
     */
    @FieldCompare(fieldName = "问题原因")
    private String problemReason;

    /**
     * 解决方案
     */
    @FieldCompare(fieldName = "解决方案")
    private String solveScheme;

    /**
     * 修复失败原因
     */
    @FieldCompare(fieldName = "修复失败原因")
    private String repairFailReason;

    /**
     * 重新打开原因
     */
    @FieldCompare(fieldName = "重新打开原因")
    private String openAgainReason;

    /**
     * 详细版本号
     */
    @FieldCompare(fieldName = "详细版本号")
    private String detailVersionId;

    /**
     * 预计上线日期
     */
    @FieldCompare(fieldName = "预计上线日期")
    private Date expectLaunchDate;

    /**
     * 所在大区
     */
    @FieldCompare(fieldName = "所在大区", enumClass = AreaEnum.class)
    private Integer area ;

    /**
     * 问题类别
     */
    @FieldCompare(fieldName = "问题类别", enumClass = BugOnlineCategoryEnum.class)
    private Integer category;
    
    /**
     * bug原因
     */
    @FieldCompare(fieldName = "bug原因")
    private String reasonName;

    /**
     * 驳回原因
     */
    @FieldCompare(fieldName = "驳回原因")
    private String dismissCauseName;

}