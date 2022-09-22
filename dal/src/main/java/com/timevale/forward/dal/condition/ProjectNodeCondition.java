package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class ProjectNodeCondition {
    /**
     * 项目节点名称
     */
    private String nodeName;
    /**
     * 节点实际时间左区间
     */
    private Date actualDateLeft;
    /**
     * 节点实际时间右区间
     */
    private Date actualDateRight;

    /**
     * 项目id
     */
    List<Long> projectIds;

}
