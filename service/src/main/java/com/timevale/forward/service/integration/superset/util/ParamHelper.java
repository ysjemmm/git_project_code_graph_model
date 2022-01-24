package com.timevale.forward.service.integration.superset.util;

import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.base.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParamHelper {
    private List<Map<String, Object>> params = new ArrayList<>();
    private static final String IS_NOT_NULL = "is not null";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_VALUE = "value";
    private static final String ASC = "asc";
    private static final String PAGE = "page";
    private static final String PAGESIZE = "pageSize";
    private static final String OFFSET = "offset";
    private static final String LIMIT = "limit";
    private static final String ORDERBY = "orderBy";

    private ParamHelper() {
    }

    public static ParamHelper newInstance() {
        return new ParamHelper();
    }

    public static ParamHelper newInstance(List<Map<String, Object>> params) {
        ParamHelper instance = new ParamHelper();
        instance.params.addAll(params);
        return instance;
    }

    public ParamHelper equals(String field, String value) {
        return append(field, value);
    }

    public ParamHelper like(String field, String value) {
        return append(field, value);
    }

    public ParamHelper greaterThanAndEquals(String field, String value) {
        return append(field, value);
    }

    public ParamHelper lessThan(String field, String value) {
        return append(field, value);
    }

    public ParamHelper in(String field, List<String> value) {
        return append(field, value);
    }

    public ParamHelper isNotNull(String field) {
        return append(field, IS_NOT_NULL);
    }

    public ParamHelper page(Integer value) {
        return append(PAGE, value);
    }

    public ParamHelper pageSize(Integer value) {
        return append(PAGESIZE, value);
    }


    public ParamHelper offset(Integer value) {
        return append(OFFSET, value);
    }

    public ParamHelper limit(Integer value) {
        return append(LIMIT, value);
    }


    public ParamHelper orderBy(String value) {
        return append(ORDERBY, value);
    }

    public ParamHelper asc(String value) {
        return append(ASC, value);
    }

    private ParamHelper append(String field, Object value) {
        Map<String, Object> param = new HashMap<>();
        param.put(FIELD_NAME, field);
        if (value instanceof String && StringUtils.isNotEmpty((String) value)) {
            param.put(FIELD_VALUE, value);
            params.add(param);
        }
        if (value instanceof Integer) {
            param.put(FIELD_VALUE, value);
            params.add(param);
        }
        if (value instanceof List && CollectionUtils.isNotEmpty((List<String>) value)) {
            param.put(FIELD_VALUE, " ('" + String.join("','", (List<String>) value) + "') ");
            params.add(param);
        }
        return this;
    }

    public List<Map<String, Object>> params() {
        return this.params;
    }
}
