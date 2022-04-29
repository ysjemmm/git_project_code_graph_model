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
public class HomePageRiskWarningSubmitTestDTO {
    /**
     * 项目id
     */
    @JSONField(name = "id")
    private Long projectId;

    /**
     * 主体id
     */
    @JSONField(name = "main_id")
    private Long mainId;

    /**
     * 风险类型
     */
    @JSONField(name = "judge_type")
    private Integer riskType;

    /**
     * 项目名称
     */
    @JSONField(name = "name")
    private String projectName;

    /**
     * 项目计划上线时间
     */
    @JSONField(name = "plan_end_date")
    private Date planEndDate;

    /**
     * 提测单名称
     */
    @JSONField(name = "bill_name")
    private String testBillName;

    /**
     * 提测结果
     */
    @JSONField(name = "bill_result")
    private String testBillResult;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HomePageRiskWarningSubmitTestDTO that = (HomePageRiskWarningSubmitTestDTO) o;
        return Objects.equal(projectId, that.projectId) && Objects.equal(testBillName, that.testBillName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(projectId, testBillName);
    }
}
