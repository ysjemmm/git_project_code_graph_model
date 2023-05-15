package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;


/**
 * @author by YangXu
 * @date 2023/05/15 15:13
 */
@Getter
@Setter
public class BizRecordDO extends BaseDO {
    /**
     * 主体id
     */
    private Long mainId;

    /**
     * 内容变更记录类型:2项目,3产品需求,4业务需求
     */
    private Integer mainType;

    /**
     * json记录
     */
    private String record;
}
