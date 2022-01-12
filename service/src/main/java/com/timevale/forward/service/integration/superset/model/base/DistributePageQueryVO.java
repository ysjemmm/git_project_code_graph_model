package com.timevale.forward.service.integration.superset.model.base;


import com.timevale.forward.service.integration.superset.config.DistributeConfigVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author yuankai
 * @date 2021/6/16 18:14
 */
@Data
@Builder
public class DistributePageQueryVO {
    /**
     * 搜索条件
     */
    private List<Map<String, Object>> params;

    /**
     * 数据库配置，api
     */
    private DistributeConfigVO distributeConfigVO;
}

