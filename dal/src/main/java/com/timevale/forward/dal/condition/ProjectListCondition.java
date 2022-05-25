package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
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
     * 项目节点状态：0 开始规划,10 需求内审,20 需求串讲,30 技术详设评审,40 开发开始,50 编写测试用例,60 用例评审,70 提测,80 测试开始,90 发布模拟,100 发布正式
     */
    private List<Long> nodeStatusList;

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

    /**
     * 创建时间左区间
     */
    private Date createDateLeft;

    /**
     * 创建时间右区间
     */
    private Date createDateRight;

}
