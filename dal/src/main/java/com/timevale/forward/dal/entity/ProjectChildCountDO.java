package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author jingchun
 * created on 2023/2/9
 */
@Getter
@Setter
public class ProjectChildCountDO {

    // 项目id
    private Long id;

    // 子项目数量
    private Long childCount;

}
