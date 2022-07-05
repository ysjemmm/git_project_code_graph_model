package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("需求查询结果-产品线分析")
public class QueryResultVO<T> extends ToString {

    @ApiModelProperty("产品线分析列表")
    private List<ProductLineAnalyseVO> analyseVOList;

    @ApiModelProperty("分页查询列表")
    private PageQueryResult<T> pageQueryResult;
}
