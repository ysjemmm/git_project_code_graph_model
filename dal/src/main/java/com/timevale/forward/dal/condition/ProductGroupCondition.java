package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.entity.ProductDemandGroupFieldDO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.function.Function;

@Data
@AllArgsConstructor
public class ProductGroupCondition {

    private String fieldName;

    // 字段提取函数
    private Function<ProductDemandGroupFieldDO, Object> fieldExtractor;

    // 值到分组的映射（可选）
    private Map<Long, String> valueMapping;
}