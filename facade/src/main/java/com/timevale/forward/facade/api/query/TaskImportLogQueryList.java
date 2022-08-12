package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/08/12 14:14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点导入记录查询")
public class TaskImportLogQueryList extends QueryBase {
}
