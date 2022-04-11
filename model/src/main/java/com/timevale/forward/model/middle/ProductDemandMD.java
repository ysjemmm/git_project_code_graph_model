package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.PriorityEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDemandMD extends BaseMD{
    /**
     * bug标题
     */
    @FieldCompare(fieldName = "需求主题")
    private String name;


    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    @FieldCompare(fieldName = "优先级",enumClass = PriorityEnum.class)
    private Integer priority;


    /**
     * 需求负责人
     */
    @FieldCompare(fieldName = "需求负责人")
    private String owner;

    /**
     * 需求描述
     */
    @FieldCompare(fieldName = "需求描述")
    private String desc;
}