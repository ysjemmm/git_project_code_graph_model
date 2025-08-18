package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;

/**
 * @author qiyuan
 * @date 2025-08-14 13:58
 **/
@Data
@Builder
public class ViewsListCondition {
    private Integer ownerType;

    private Integer type;

    private String ownerId;

    private Boolean hidden;
}
