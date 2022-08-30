package com.timevale.forward.dal.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/06/28 10:32
 */
@Data
public class UpdateTimeDTO{
    /**
     * 更新时间
     */
    @JSONField(name = "update_time")
    private Date updateTime;
}
