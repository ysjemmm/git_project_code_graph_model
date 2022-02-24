package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class ProjectListCondition extends QueryBase {
    /**
     * id
     */
    private Long id;

    /**
     * 是否为客户开发项目：0否，1是
     */
    private Integer customerDev;

    /**
     * id
     */
    private List<Long> ids;

    /**
     * productDemandId
     */
    private Long productDemandId;

    /**
     * 名称
     */
    private String name;

    /**
     * 优先级:0(P0),1(P1),2(P2)
     */
    private List<Integer> priorities;

    /**
     * 业务域
     */
    private List<Long> bizDomainIds;

    /**
     * 产品线
     */
    private List<Long> productLineIds;

    /**
     * 项目类型:0产品研发项目,1技术优化项目,2日常迭代
     */
    private List<Integer> types;

    /**
     * 项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废
     */
    private List<Integer> status;

    /**
     * 项目经理
     */
    private List<String> pms;

    /**
     * 产品经理
     */
    private List<String> pds;

    /**
     * 团队成员
     */
    private List<String> teamMembers;

    /**
     * 项目计划开始时间左区间
     */
    private Date planStartDateLeft;

    /**
     * 项目计划开始时间右区间
     */
    private Date planStartDateRight;

    /**
     * 项目计划结束时间左区间
     */
    private Date planEndDateLeft;

    /**
     * 项目计划结束时间右区间
     */
    private Date planEndDateRight;

    /**
     * 项目实际开始时间左区间
     */
    private Date actualStartDateLeft;

    /**
     * 项目实际开始时间右区间
     */
    private Date actualStartDateRight;

    /**
     * 项目实际结束时间左区间
     */
    private Date actualEndDateLeft;

    /**
     * 项目实际结束时间右区间
     */
    private Date actualEndDateRight;

}
