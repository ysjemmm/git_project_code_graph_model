package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 15:20
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求查询")
public class BizDemandQueryList extends QueryBase {

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("业务需求id")
    private Long id;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private List<Byte> priorityList;

    @ApiModelProperty("业务域id")
    private List<Long> bizDomainIdList;

    @ApiModelProperty("产品线id")
    private List<Long> productLineIdList;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("预期上线时间")
    private List<Byte> planReleaseDateList;

    @ApiModelProperty("需求解决状态")
    private List<Byte> statusList;

    @ApiModelProperty("需求提交人")
    private List<PersonQuery> createManInfoList;

    @ApiModelProperty("需求接收人")
    private List<PersonQuery> receiveManInfoList;

    @ApiModelProperty("需求部门id")
    private List<Long> deptIdList;
    
    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;

}
