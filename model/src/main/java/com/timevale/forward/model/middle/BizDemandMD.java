package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.BizDemandPriorityEnum;
import com.timevale.forward.model.enums.PlanReleaseDateEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


/**
 * @author yangxu
 * @date 2022/04/12
 */
@Data
public class BizDemandMD extends BaseMD {

    @FieldCompare(fieldName = "需求主题")
    private String name;

    @FieldCompare(fieldName = "优先级", enumClass = BizDemandPriorityEnum.class)
    private Integer priority;

    @FieldCompare(fieldName = "影响数据指标")
    private String dataIndicators;

    @FieldCompare(fieldName = "目标客户/用户/项目")
    private String targetCustomer;

    @FieldCompare(fieldName = "需求接收人")
    private String receiveMan;

    @FieldCompare(fieldName = "需求提交人")
    private String submitMan;

    @FieldCompare(fieldName = "共创用户", enumClass = YesOrNoEnum.class)
    private Boolean createCustomer;

    @FieldCompare(fieldName = "需求描述")
    private String desc;

    @FieldCompare(fieldName = "预期上线时间", enumClass = PlanReleaseDateEnum.class)
    private Integer planReleaseDate;
}
