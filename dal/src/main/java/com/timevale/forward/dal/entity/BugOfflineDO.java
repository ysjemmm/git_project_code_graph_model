package com.timevale.forward.dal.entity;

import com.timevale.forward.dal.annotation.FieldCompare;
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
    @FieldCompare(fieldName = "优先级",enumMapping ="{0:\"紧急\",10:\"高\",20:\"中\",30:\"低\"}")
    private Integer priority;
    /**
     * bug来源不能为空
     */
    private Integer source;
    /**
     * bug原因不能为空
     */
    private Integer reason;
    /**
     * bug所属端
     */
    private Integer belong;
    /**
     * bug环境
     */
    private Integer env;
    /**
     * 复现频率
     */
    private Integer frequency;
    /**
     * 描述
     */
    private String desc;

}
