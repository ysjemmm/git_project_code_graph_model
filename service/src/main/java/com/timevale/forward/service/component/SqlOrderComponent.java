package com.timevale.forward.service.component;

/**
 * @author by YangXu
 * @date 2022/06/24 15:34
 */
public interface SqlOrderComponent {

    /**
     * 构建
     *
     * @param field 字段
     * @param order 排序 0正序，1逆序
     * @return {@link String}
     */
    String build(String field, Integer order);

    /**
     * 构建 无需id
     */
    String buildWithoutId(String field, Integer order);
}
