package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.google.common.base.Objects;
import lombok.Data;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/01/24 13:54
 */
@Data
public class HomePageRiskWarningDTO {
    /**
     * 项目id
     */
    @JSONField(name = "project_id")
    private Long projectId;

    /**
     * 节点id
     */
    @JSONField(name = "node_id")
    private Long nodeId;

    /**
     * 风险类型
     */
    @JSONField(name = "judge_type")
    private Integer riskType;

    /**
     * 项目名称
     */
    @JSONField(name = "project_name")
    private String projectName;

    /**
     * 项目计划上线时间
     */
    @JSONField(name = "plan_end_date")
    private Date planEndDate;

    /**
     * 项目计划上线时间
     */
    @JSONField(name = "node_actual_date")
    private Date nodeActualDate;

    /**
     * 节点名称
     */
    @JSONField(name = "node_name")
    private String nodeName;

    /**
     * 逾期类型
     */
    @JSONField(name = "flag")
    private String overdueType;

    /**
     * 逾期天数
     */
    @JSONField(name = "latedate")
    private String overdueDay;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HomePageRiskWarningDTO that = (HomePageRiskWarningDTO) o;
        return Objects.equal(projectId, that.projectId) && Objects.equal(nodeName, that.nodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(projectId, nodeName);
    }
}
