package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
public class TaskTimeDTO {

    /**
     * 使用时间
     */
    @JSONField(name = "consum_time")
    private BigDecimal useTime;

    /**
     * 任务执行人名称
     */
    @JSONField(name = "user_name")
    private String userName;

}