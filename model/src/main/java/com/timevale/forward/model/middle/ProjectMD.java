package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProjectTypeEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
     * 是否为客户开发项目：0否，1是
     */
    @FieldCompare(fieldName = "是否为客开项目",enumClass = YesOrNoEnum.class)
    private Integer customerDev;

    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    @FieldCompare(fieldName = "优先级",enumClass = PriorityEnum.class)
    private Integer priority;

    /**
     * 0产品研发项目,1技术优化项目,2日常迭代
     */
    @FieldCompare(fieldName = "项目类型",enumClass = ProjectTypeEnum.class)
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
}