package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.facade.api.request.BaseReq;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.model.enums.BizDemandPriorityEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;


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

    @FieldCompare(fieldName = "接收人")
    private String receiveMan;

    @ApiModelProperty("是否共创用户")
    @NotNull(message = "共创用户不能为空")
    @FieldCompare(fieldName = "共创用户", enumClass = YesOrNoEnum.class)
    private Boolean createCustomer;

    @FieldCompare(fieldName = "需求描述")
    private String desc;
}
