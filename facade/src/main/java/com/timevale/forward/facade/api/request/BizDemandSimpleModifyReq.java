package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Set;

/**
 * @author dove
 * created on 2024-04-2024/4/1
 */
@Getter
@Setter
@Accessors(chain = true)
public class BizDemandSimpleModifyReq extends ToString {

    /**
     * 业务需求id列表
     */
    private Set<Long> bizDemandIds;

    /**
     * 是否客开需求
     */
    private Boolean customerDevDemand;

}
