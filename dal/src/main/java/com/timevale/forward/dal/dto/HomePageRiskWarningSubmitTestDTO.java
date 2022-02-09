package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
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
}
