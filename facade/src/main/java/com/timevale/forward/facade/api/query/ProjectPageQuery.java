package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2023/03/02 15:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目分页查询")
public class ProjectPageQuery extends QueryBase {

    @ApiModelProperty("名称")
    private String name;
}
