package com.timevale.forward.service.integration.superset.constant;

/**
 * @author yuankai
 * @date 2020/11/18 11:38
 */
public class QueryConstant {

    /**
     * 查询列表
     */
    public static final String QUERY_MODE_RAW = "raw";

    /**
     * 查询数量
     */
    public static final String QUERY_MODE_COUNT = "count";

    /**
     * 集合查询
     */
    public static final String QUERY_MODE_AGGREGATE = "aggregate";

    /**
     * 请求url
     */
    public static final String URL = "?form_data={form_data}&results={results}&token={token}";

    public static final String SUPERSET_SUCCESS = "success";
    public static final String SUPERSET_STATUS = "status";
}
