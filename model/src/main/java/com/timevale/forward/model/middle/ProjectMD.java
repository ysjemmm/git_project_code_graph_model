package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectMD extends BaseMD{
    /**
     * bug标题
     */
    @FieldCompare(fieldName = "项目名称")
    private String name;

    @FieldCompare(fieldName = "项目计划开始时间")
    private Date planStartDate;

    @FieldCompare(fieldName = "项目计划结束时间")
    private Date planEndDate;

    @FieldCompare(fieldName = "项目实际开始时间")
    private Date actualStartDate;

    @FieldCompare(fieldName = "项目实际结束时间")
    private Date actualEndDate;

    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    @FieldCompare(fieldName = "优先级",enumClass = PriorityEnum.class)
    private Integer priority;

    /**
     * 0产品研发项目,1技术优化项目,2日常迭代
     */
    @FieldCompare(fieldName = "项目性质",enumClass = ProjectTypeEnum.class)
    private Integer type;

    /**
     * 项目经理
     */
    @FieldCompare(fieldName = "项目经理")
    private String pmName;

    /**
     * 项目描述
     */
    @FieldCompare(fieldName = "项目描述")
    private String desc;

    /**
     * 是否发布平台发布
     */
    @FieldCompare(fieldName = "是否需要在发布平台发布",enumClass = YesOrNoEnum.class)
    private Integer isPlatformPublish;

    @FieldCompare(fieldName = "是否有项目目标",enumClass = YesOrNoEnum.class)
    private Integer isWithGoal;

    @FieldCompare(fieldName = "项目等级",enumClass = ProjectLevelEnum.class)
    private Integer level;

    @FieldCompare(fieldName = "立项工作量评估（人天）", scale = 2)
    private BigDecimal resourceAssessment;

    @FieldCompare(fieldName = "立项开始时间")
    private Date pjEstablishStartDate;

    @FieldCompare(fieldName = "立项预期上线时间")
    private Date pjEstablishPublishDate;

    @FieldCompare(fieldName = "项目暂停原因")
    private String suspendReason;

    @FieldCompare(fieldName = "是否需要项目验收",enumClass = YesOrNoEnum.class)
    private Integer isAcceptance;

    @FieldCompare(fieldName = "项目类型", enumClass = ProjectInnerTypeEnum.class)
    private Integer innerType;

    @FieldCompare(fieldName = "项目类型", enumClass = ProjectKindEnum.class)
    private Integer kind;

    @FieldCompare(fieldName = "SR")
    private String sr;

    @FieldCompare(fieldName = "项目负责人")
    private String principal;

    @FieldCompare(fieldName = "1-N产研团队负责人")
    private String otnPrincipal;
}