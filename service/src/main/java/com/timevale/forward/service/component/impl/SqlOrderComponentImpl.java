package com.timevale.forward.service.component.impl;

import cn.hutool.core.util.StrUtil;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.mandarin.base.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2022/06/24 15:34
 */
@Component
public class SqlOrderComponentImpl implements SqlOrderComponent {

    private static final String DESC = "desc";

    private static final String BLANK = " ";

    private static final String SUFFIX = ",id desc";

    @Override
    public String build(String field, Integer order) {
        if(StringUtils.isEmpty(field)){
            return CommonConstant.DEFAULT_ORDER_BY;
        }

        // 驼峰转下划线
        field = StrUtil.toUnderlineCase(field);

        StringBuilder sql = new StringBuilder();
        // 排序字段
        sql.append(field);

        // 判断正逆序
        if(order == 1){
            sql.append(BLANK).append(DESC);
        }

        // 排序id唯一性
        sql.append(SUFFIX);

        return sql.toString();
    }

    @Override
    public String buildWithoutId(String field, Integer order) {
        if(StringUtils.isEmpty(field)){
            return StringUtils.EMPTY;
        }

        // 驼峰转下划线
        field = StrUtil.toUnderlineCase(field);

        StringBuilder sql = new StringBuilder();
        // 排序字段
        sql.append(field);

        // 判断正逆序
        if(order == 1){
            sql.append(BLANK).append(DESC);
        }
        return sql.toString();
    }

}
