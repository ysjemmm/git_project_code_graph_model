package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/8
 */
@Getter
@Setter
public class ProjectMilestoneListVO extends ToString {

    private List<Integer> validStages;

    private List<ProjectMilestoneVO> list;

}
