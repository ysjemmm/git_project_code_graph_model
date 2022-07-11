package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ProjectIdPageQuery extends QueryBase {

    @NotNull(message = "项目id不能为空")
    private Long projectId;

}
