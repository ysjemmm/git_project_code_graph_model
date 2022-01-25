package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/01/25 16:44
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-近三周计划上线项目")
public class HomePageProjectOnlineLatelyQueryList extends QueryBase {
}
