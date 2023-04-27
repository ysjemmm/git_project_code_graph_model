package com.timevale.forward.dal.entity;

import com.timevale.mandarin.base.util.JsonUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectDO extends BaseDO {
    /**
     * name
     */
    private String name;

    /**
     * 是否为客户开发项目：0否，1是
     */
    private Integer customerDev;

    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    private Integer priority;
    /**
     * 类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求
     */
    private Integer type;

    /**
     * 项目状态:0待启动,10规划中,15执行中,20研发中,25收尾中,30测试中,35运营中,40已发布,45已完成,-10已暂停,-20已作废
     */
    private Integer status;

    /**
     * 0 开始规划,10 需求内审,20 需求串讲,30 技术详设评审,40 开发开始,50 编写测试用例,60 用例评审,70 提测,80 测试开始,90 发布模拟,100 发布正式
     */
    private Integer nodeStatus;

    /**
     * 产品线
     */
    private List<Long> productLineIds;

    /**
     * pm名称
     */
    private String pm;

    /**
     * pm
     */
    private String pmId;

    /**
     * 项目计划开始时间
     */
    private Date planStartDate;
    /**
     * 项目计划结束时间
     */
    private Date planEndDate;

    /**
     * 项目实际开始时间
     */
    private Date actualStartDate;
    /**
     * 项目实际结束时间
     */
    private Date actualEndDate;

    /**
     * 描述
     */
    private String desc;

    /**
     * 产品经理
     */
    private List<PersonDO> pds;

    /**
     * 是否在发布平台发布
     */
    private Integer isPlatformPublish;

    /**
     * 是否有项目目标
     */
    private Integer isWithGoal;

    /**
     * 项目等级：0普通，10重点，20S级别，30A级别，40B级别
     */
    private Integer level;

    /**
     * 产品资源评估（人天）
     */
    private BigDecimal resourceAssessment;

    /**
     * 立项开始时间
     */
    private Date pjEstablishStartDate;


    /**
     * 立项预期上线时间
     */
    private Date pjEstablishPublishDate;

    /**
     * 项目暂停原因
     */
    private String suspendReason;

    /**
     * 项目作废原因
     */
    private String invalidReason;

    /**
     * 是否需要验收
     */
    private Integer isAcceptance;

    /**
     * 文档未填写原因
     */
    private String unWriteReason;

    /**
     * 项目类型 0:产研项目; 1:内部项目
     */
    private Integer category;

    /**
     * 项目预计收益金额（元）
     */
    private BigDecimal expectedIncome;

    /**
     * 父节点id列表，用逗号分隔，包含自身
     */
    private String parentIds;

    /**
     * 内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目
     */
    private Integer innerType;

    /**
     * 有效阶段
     */
    private String validStages;

    /**
     * 父级项目id
     */
    private Long parentId;

    /**
     * 项目类型，0-空，1-PBG项目/基线项目，2-PBG项目/1-N客开项目，3-职能后台项目/流程IT中心项目
     */
    private Integer kind;

    /**
     * sr
     */
    private String sr;

    /**
     * sr Id
     */
    private String srId;

    /**
     * 项目负责人
     */
    private String principal;

    /**
     * 项目负责人id
     */
    private String principalId;

    /**
     * 1-n负责人
     */
    private String otnPrincipal;

    /**
     * 1-n负责人Id
     */
    private String otnPrincipalId;

    /**
     * 结项时间
     */
    private Date conclusionDate;

    /**
     * sr建议评价等级
     */
    private Integer srEvaluateGrade;

    /**
     * 来源交付项目id
     */
    private String sourceId;

    /**
     * 返回父节点id列表
     */
    public List<Long> getParentList() {
        return Optional.ofNullable(parentIds).filter(StringUtils::isNotBlank)
                .map(pIds -> Stream.of(pIds.substring(1, pIds.length() - 1).split(","))
                        // prevent in case someone directly update table with space accidentally
                        .map(String::trim)
                        .map(Long::parseLong)
                        .collect(Collectors.toList())).orElse(Collections.emptyList());
    }

    public List<Integer> getValidStageList() {
        if (StringUtils.isEmpty(validStages)) {
            return Collections.emptyList();
        }
        return JsonUtils.json2list(validStages, Integer.class);
    }

    public void setValidStageList(List<Integer> stageList) {
        validStages = "[" +
                stageList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) +
                "]";
    }

    public Long getParentId() {
        List<Long> parentIds = getParentList();
        if (parentIds.size() < 2) {
            return null;
        }
        return parentIds.get(parentIds.size() - 2);
    }

    public void setParentIds(List<Long> parentIds) {
        this.parentIds = "," +
                parentIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) +
                ",";
    }

    // 请勿删除此方法，该方法会被mybatis使用放入数据
    public void setParentIds(String parentIds) {
        this.parentIds = parentIds;
    }

}
