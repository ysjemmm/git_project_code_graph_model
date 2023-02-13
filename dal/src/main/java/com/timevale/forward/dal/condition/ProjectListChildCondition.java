package com.timevale.forward.dal.condition;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;

/**
 * @author jingchun
 * created on 2023/2/6
 */
@Getter
@Setter
@Accessors(chain = true)
public class ProjectListChildCondition {
    // 根节点项目id
    private Long projectId;

    // 节点树所在id
    private String navigateParentIdsPrefix;

    // 项目名称
    private String projectName;

    // 子项目id
    private Long childProjectId;

    // 内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目
    private Collection<Integer> innerTypes;

    // 项目类型:0产品研发项目,1技术优化项目,2日常迭代
    private Collection<Integer> types;

    // 项目经理
    private Collection<String> pms;

    // 项目等级
    private Collection<Integer> levels;

    // 项目状态
    private Collection<Integer> status;

    // 1-子项目列表; 2-可添加为子项目列表; 3-里程碑可关联项目列表
    private Integer searchType;

    private String parentIdsRegexp;

    // 父节点id列表
    private Collection<Long> parentIds;

    private Integer pageNum;

    private Integer pageSize;

}
