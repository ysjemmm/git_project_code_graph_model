package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class ProjectListCondition {
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
    @WildcardEscape
    private String name;

    /**
     * 类别 0-产研项目 1-内部项目
     */
    private Integer category;

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
     * 子产品线id
     */
    private List<Long> subProductLineIds;

    /**
     * 项目类型:0产品研发项目,1技术优化项目,2日常迭代
     */
    private List<Integer> types;

    /**
     * 内部项目类型
     */
    private List<Integer> innerTypes;

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
    /**
     * 打回次数判断类型
     */
    private Integer returnCountType;
    /**
     * 打回次数
     */
    private Integer returnCount;
    /**
     * 是否延期
     */
    private Boolean isDelay;

    /**
     * 提测时间左区间
     */
    private Date actualTestDateLeft;

    /**
     * 提测时间右区间
     */
    private Date actualTestDateRight;

    /**
     * 项目等级：0普通，1重点，2S级别，3A级别，4B级别
     */
    private List<Integer> levels;

    /**
     * 是否有项目风险
     */
    private Boolean includeRisk;

    /**
     * 排序字段
     */
    private String orderFiled;

    /**
     * 排序规则：0正序，1逆序
     */
    private Integer orderCollation;

    /**
     * 是否包含标签
     */
    private Boolean containLabel;

    /**
     * 标签id
     */
    private List<Long> labelIds;

    /**
     * 项目节点code
     */
    private Integer nodeCode;
    /**
     * 节点实际时间左区间
     */
    private Date actualDateLeft;
    /**
     * 节点实际时间右区间
     */
    private Date actualDateRight;

    private Boolean onlyFirstLevel;

    /**
     * srs
     */
    private List<String> srs;

    /**
     * 项目类型: 0-空，1-PBG项目/基线项目，2-PBG项目/1-N客开项目，3-职能后台项目/流程IT中心项目
     */
    private Integer kind;

    /**
     * 来源id (交付项目id)
     */
    private String sourceId;

    /**
     * 结项时间-开始
     */
    private Date conclusionDateLeft;

    /**
     * 结项时间-结束
     */
    private Date conclusionDateRight;

    /**
     * pbu id
     */
    private Collection<Long> pbuIds;

    /**
     * 是否需要验收
     */
    private Boolean isAcceptance;

    /**
     * 项目验收状态:0-无需验收，1-未发起验收，2-已发起验收，3-验收完毕
     */
    private Integer projectAcceptanceStatus;

    /**
     * 是否包含关联分组, true: 搜索未关联分组的项目，false: 所有
     */
    private Boolean unLinkGroup;

    /**
     * 是否发送工时通知
     */
    private Integer workHoursNotify;

    private Integer pageNum = 1;

    private Integer pageSize = 20;

}
