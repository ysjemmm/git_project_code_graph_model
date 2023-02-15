package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/06/02 10:49
 */
@Data
@Builder
public class ProjectRiskCondition{

    /**
     * 主体id
     */
    private Long mainId;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 状态列表
     */
    private List<Integer> statusList;

    /**
     * 类型列表
     */
    private List<Integer> typeList;
}
