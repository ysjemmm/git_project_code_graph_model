package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
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
@ApiModel("产品需求详情")
public class ProductDemandDetailVO extends ToString {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    private Byte priority;

    @ApiModelProperty("产品线")
    private Long productLineId;

    @ApiModelProperty("类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求")
    private Byte type;

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废")
    private Byte status;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("项目")
    private ProjectVO projectVO;

    @ApiModelProperty("业务需求")
    private List<BizDemandVO> bizDemandVO;

    @ApiModelProperty("抄送人")
    private List<PersonVO> recipients;

    @ApiModelProperty("附件")
    private List<FileVO> files;
    
}
