package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求查询")
public class ProductDemandQueryList extends QueryBase {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    private List<Byte> priorities;

    @ApiModelProperty("业务域")
    private List<Long> bizDomainIds;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废")
    private Byte status;

    @ApiModelProperty("负责人")
    private List<String> owners;

    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;
}
