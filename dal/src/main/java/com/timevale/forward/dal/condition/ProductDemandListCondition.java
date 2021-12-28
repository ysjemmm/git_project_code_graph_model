package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求查询条件")
public class ProductDemandListCondition extends QueryBase {
    /**
     * id
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 业务域
     */
    private List<Long> bizDomainIds;

    /**
     * 产品线
     */
    private List<Long> productLineIds;

    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    private List<Integer> priorities;

    /**
     * 需求状态:0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废
     */
    private List<Integer> status;

    /**
     * 负责人
     */
    private List<String> ownerIds;

    /**
     * 创建人
     */
    private List<String> createManIds;

    /**
     * 抄送人
     */
    private String copierId;

    /**
     * 创建时间
     */
    private Date createDateStart;

    /**
     * 创建时间
     */
    private Date createDateEnd;
    /**
     * 修改时间开始
     */
    private Date modifyDateStart;
    /**
     * 修改时间结束
     */
    private Date modifyDateEnd;
    /**
     * 项目-产品列表弹框 找出没有被关联的产品需求
     */
    private Boolean matchProductDemand;

}
