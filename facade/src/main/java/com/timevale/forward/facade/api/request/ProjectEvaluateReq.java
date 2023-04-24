package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/06/24 09:52
 */
@Getter
@Setter
@ApiModel("项目评价请求")
public class ProjectEvaluateReq extends ToString {

    @ApiModelProperty("项目评价项")
    @Valid
    @NotEmpty
    private List<EvaluateReq> evaluateReqList;

    @ApiModelProperty("SR建议评价等级，A-20，B-30，C-40")
    private Integer srEvaluateGrade;

}
