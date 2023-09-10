package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务需求耗时实体
 *
 * @author yangxu
 * @date 2023/09/10
 */
@Getter
@Setter
public class BizTimeDO extends BaseDO{

    private String userName;

    private String status;

    private Long mainId;

    private Integer mainType;

    private Long time;
}
