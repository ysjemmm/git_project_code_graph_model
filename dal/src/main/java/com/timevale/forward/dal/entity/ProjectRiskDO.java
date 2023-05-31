package com.timevale.forward.dal.entity;

import com.google.common.base.Objects;
import lombok.Data;


/**
 * @author by YangXu
 * @date 2022/04/29 16:27
 */
@Data
public class ProjectRiskDO extends BaseDO {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 主体id
     */
    private Long mainId;

    /**
     * 风险类型 0 其它， 10 项目关键节点逾期 20 提测质量不达标 30 任务逾期
     */
    private Integer type;

    /**
     * 名称
     */
    private String name;

    /**
     * 风险标志
     */
    private String sign;

    /**
     * 状态：0 待处理, 1 已处理
     */
    private Integer status;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectRiskDO riskDO = (ProjectRiskDO) o;
        return Objects.equal(projectId, riskDO.projectId)
                && Objects.equal(mainId, riskDO.mainId)
                && Objects.equal(type, riskDO.type)
                && Objects.equal(name, riskDO.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(projectId, mainId, type, name);
    }
}
