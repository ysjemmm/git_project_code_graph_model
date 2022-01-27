package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

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
