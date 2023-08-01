package com.timevale.forward.model.dto;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 项目预处理和校验后数据回传用打包类
 *
 * @author jingchun
 * create on 8/1/2023
 **/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModifyProjectProcessedBundle {

    /**
     * 目前存在于数据库中的项目信息
     */
    private ProjectDO oldProject;

    /**
     * 根据传入参数构造的新项目信息
     */
    private ProjectDO newProject;

    /**
     * 新项目节点信息
     */
    private List<ProjectNodeDO> nodes;

    /**
     * 存放由于代码耦合问题造成的需要更新数据库的任务列表
     */
    private List<Runnable> delayTasks;

}
