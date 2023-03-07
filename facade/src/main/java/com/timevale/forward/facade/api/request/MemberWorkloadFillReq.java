package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/06/24 09:52
 */
@Getter
@Setter
@ApiModel("成员工作量填报请求")
public class MemberWorkloadFillReq extends ToString {

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id必填")
    private Long projectId;

    @ApiModelProperty("成员工作量信息")
    @Valid
    private List<MemberWorkloadModifyReq> modifyReqList;

    @ApiModelProperty("变更事由")
    private String changeReason;

    @ApiModelProperty("PBU负责人")
    private PersonAddReq pbuPrincipal;
}
