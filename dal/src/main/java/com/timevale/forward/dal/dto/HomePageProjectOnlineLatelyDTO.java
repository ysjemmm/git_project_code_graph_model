package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/01/21 17:50
 */
@Data
public class HomePageProjectOnlineLatelyDTO {

    /**
     * 项目id
     */
    @JSONField(name = "project_id")
    private Long projectId;

    /**
     * 项目名称
     */
    @JSONField(name = "pj_name")
    private String projectName;

    /**
     * 项目经理
     */
    @JSONField(name = "pm")
    private String pm;

    /**
     * 状态
     */
    @JSONField(name = "status")
    private String status;

    /**
     * 项目计划上线时间
     */
    @JSONField(name = "plan_start_date")
    private Date planStartDate;
}
