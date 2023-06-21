package com.timevale.forward.service.constant;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
public class CommonConstant {

    public static final String APP = "forward";

    public static final String JOIN_LINE = "-";

    public static final String WIDE_COLON = "：";

    public static final String TILDE = "~";

    public static final String BLANK = " ";

    public static final String DEFAULT_ORDER_BY = " modify_date desc, id";

    public static final String CREATE_DESC_ORDER_BY = " create_date desc, id";

    public static final String CREATE_ORDER_BY = " create_date, id";

    public static final String PROJECT_RISK_ORDER_BY = "IF(status = 0, 0, IF(status = 1, 1, 2)), modify_date desc, id";

    public static final String SECONDS_PER_HOUR = "3600";

    public static final String TESTBILL_SUFFIX = "提测单";

    public static final String NULL = "无";

    public static final Integer REASON_NULL = 0;

    public static final String DAY = "天";

    public static final String SYSTEM = "SYSTEM-SYSTEM";

    public static final String SYSTEM_DEFAULT = "系统默认";

    public static final String PMO = "PMO";

    public static final String FORWARD_BIZ_RELATION_TOPIC = "forward-biz-rel";

    public static final String INVALID = "已作废";

    public static final Integer DESC_MAX_LENGTH = 20000;
}
