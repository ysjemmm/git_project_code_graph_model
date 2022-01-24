package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author by YangXu
 * @date 2022/01/24 13:54
 */
@Data
public class HomePageRiskWarningDTO {
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
     * 节点名称
     */
    @JSONField(name = "node")
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

    /**
     * 提测单名称
     */
    private String testBillName;
}
