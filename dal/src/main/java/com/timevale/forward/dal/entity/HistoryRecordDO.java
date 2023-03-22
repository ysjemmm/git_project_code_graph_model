package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;


/**
 * @author by YangXu
 * @date 2023/03/10 14:35
 */
@Getter
@Setter
@Accessors(chain = true)
public class HistoryRecordDO extends BaseDO {
    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 记录内容
     */
    private String recordContent;

    /**
     * 版本
     */
    private BigDecimal version;
}
