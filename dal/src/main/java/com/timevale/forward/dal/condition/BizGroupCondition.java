package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.entity.BizDemandGroupFieldDO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.function.Function;

@Data
@AllArgsConstructor
public class BizGroupCondition {

    private String fieldName;

    // 字段提取函数
    private Function<BizDemandGroupFieldDO, Object> fieldExtractor;
}