package com.timevale.forward.service.utils.date;

/**
 * @author yuankai
 * @date 2020/11/25 10:22
 */
public enum DateStyle {
    /**
     * Yyyy mm date style.
     */
    YYYY_MM("yyyy-MM", false),

    /**
     * Yyyy mm dd date style.
     */
    YYYY_MM_DD("yyyy-MM-dd", false),

    /**
     * The Yyyy mm dd hh.
     */
    YYYY_MM_DD_HH("yyyy-MM-dd HH", false),

    /**
     * The Yyyy mm dd hh mm.
     */
    YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm", false),

    /**
     * The Yyyy mm dd hh mm ss.
     */
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss", false),

    /**
     * The Yyyy mm dd hh mm ss.S
     */
    YYYY_MM_DD_HH_MM_SS_S("yyyy-MM-dd HH:mm:ss.S", false),

    /**
     * Yyyymm date style.
     */
    YYYYMM("yyyyMM", false),

    /**
     * Yyyymmdd date style.
     */
    YYYYMMDD("yyyyMMdd", false),

    /**
     * Yyyymmddhh date style.
     */
    YYYYMMDDHH("yyyyMMddHH", false),

    /**
     * Yyyymmddhhmm date style.
     */
    YYYYMMDDHHMM("yyyyMMddHHmm", false),

    /**
     * Yyyymmddhhmmss date style.
     */
    YYYYMMDDHHMMSS("yyyyMMddHHmmss", false),

    /**
     * Yyyymmddhhmmsss date style.
     */
    YYYYMMDDHHMMSSS("yyyyMMddHHmmssS", false),

    /**
     * Yyyy mm en date style.
     */
    YYYY_MM_EN("yyyy/MM", false),

    /**
     * Yyyy mm dd en date style.
     */
    YYYY_MM_DD_EN("yyyy/MM/dd", false),

    /**
     * The Yyyy mm dd hh en.
     */
    YYYY_MM_DD_HH_EN("yyyy/MM/dd HH", false),

    /**
     * The Yyyy mm dd hh mm en.
     */
    YYYY_MM_DD_HH_MM_EN("yyyy/MM/dd HH:mm", false),

    /**
     * The Yyyy mm dd hh mm ss en.
     */
    YYYY_MM_DD_HH_MM_SS_EN("yyyy/MM/dd HH:mm:ss", false),

    /**
     * The Yyyy mm dd hh mm ss en.
     */
    YYYY_MM_DD_HH_MM_SS_EN_S("yyyy/MM/dd HH:mm:ss.S", false),

    /**
     * Yyyy mm cn date style.
     */
    YYYY_MM_CN("yyyy年MM月", false),

    /**
     * Yyyy mm dd cn date style.
     */
    YYYY_MM_DD_CN("yyyy年MM月dd日", false),

    /**
     * Yyyy mm dd cn 2 date style.
     */
    YYYY_MM_DD_CN2("yyyy年MM月dd号", false),

    /**
     * Yyyy mm dd hh cn date style.
     */
    YYYY_MM_DD_HH_CN("yyyy年MM月dd日 HH点", false),

    /**
     * Yyyy mm dd hh cn 2 date style.
     */
    YYYY_MM_DD_HH_CN_2("yyyy年MM月dd日 HH时", false),

    /**
     * Yyyy mm dd hh cn 2 date style.
     */
    YYYY_MM_DD_HH_CN2("yyyy年MM月dd号 HH点", false),

    /**
     * Yyyy mm dd hh cn 2 2 date style.
     */
    YYYY_MM_DD_HH_CN2_2("yyyy年MM月dd号 HH时", false),

    /**
     * Yyyy mm dd hh mm cn date style.
     */
    YYYY_MM_DD_HH_MM_CN("yyyy年MM月dd日 HH:mm", false),

    /**
     * Yyyy mm dd hh mm cn 2 date style.
     */
    YYYY_MM_DD_HH_MM_CN_2("yyyy年MM月dd日 HH点mm分", false),

    /**
     * Yyyy mm dd hh mm cn 3 date style.
     */
    YYYY_MM_DD_HH_MM_CN_3("yyyy年MM月dd日 HH时mm分", false),

    /**
     * Yyyy mm dd hh mm cn 2 date style.
     */
    YYYY_MM_DD_HH_MM_CN2("yyyy年MM月dd号 HH:mm", false),

    /**
     * Yyyy mm dd hh mm cn 2 2 date style.
     */
    YYYY_MM_DD_HH_MM_CN2_2("yyyy年MM月dd号 HH点mm分", false),

    /**
     * Yyyy mm dd hh mm cn 2 3 date style.
     */
    YYYY_MM_DD_HH_MM_CN2_3("yyyy年MM月dd号 HH时mm分", false),

    /**
     * Yyyy mm dd hh mm ss cn date style.
     */
    YYYY_MM_DD_HH_MM_SS_CN("yyyy年MM月dd日 HH:mm:ss", false),

    /**
     * Yyyy mm dd hh mm ss cn 2 date style.
     */
    YYYY_MM_DD_HH_MM_SS_CN_2("yyyy年MM月dd日 HH点mm分ss秒", false),

    /**
     * Yyyy mm dd hh mm ss cn 3 date style.
     */
    YYYY_MM_DD_HH_MM_SS_CN_3("yyyy年MM月dd日 HH时mm分ss秒", false),

    /**
     * Yyyy mm dd hh mm ss cn 2 date style.
     */
    YYYY_MM_DD_HH_MM_SS_CN2("yyyy年MM月dd号 HH:mm:ss", false),

    /**
     * Yyyy mm dd hh mm ss cn 2 2 date style.
     */
    YYYY_MM_DD_HH_MM_SS_CN2_2("yyyy年MM月dd号 HH点mm分ss秒", false),

    /**
     * Yyyy mm dd hh mm ss cn 2 3 date style.
     */
    YYYY_MM_DD_HH_MM_SS_CN2_3("yyyy年MM月dd号 HH时mm分ss秒", false),

    /**
     * Yyyy mm dd hh mm sss cn date style.
     */
    YYYY_MM_DD_HH_MM_SSS_CN("yyyy年MM月dd日 HH:mm:ssS", false),

    /**
     * Yyyy mm dd hh mm sss cn 2 date style.
     */
    YYYY_MM_DD_HH_MM_SSS_CN_2("yyyy年MM月dd日 HH点mm分ss秒S", false),

    /**
     * Yyyy mm dd hh mm sss cn 3 date style.
     */
    YYYY_MM_DD_HH_MM_SSS_CN_3("yyyy年MM月dd日 HH时mm分ss秒S", false),

    /**
     * Yyyy mm dd hh mm sss cn 2 date style.
     */
    YYYY_MM_DD_HH_MM_SSS_CN2("yyyy年MM月dd号 HH:mm:ssS", false),

    /**
     * Yyyy mm dd hh mm sss cn 2 2 date style.
     */
    YYYY_MM_DD_HH_MM_SSS_CN2_2("yyyy年MM月dd号 HH点mm分ss秒S", false),

    /**
     * Yyyy mm dd hh mm sss cn 2 3 date style.
     */
    YYYY_MM_DD_HH_MM_SSS_CN2_3("yyyy年MM月dd号 HH时mm分ss秒S", false),

    /**
     * Mm dd date style.
     */
    MM_DD("MM-dd", true),

    /**
     * The Mm dd hh mm.
     */
    MM_DD_HH_MM("MM-dd HH:mm", true),

    /**
     * The Mm dd hh mm ss.
     */
    MM_DD_HH_MM_SS("MM-dd HH:mm:ss", true),

    /**
     * Hh mm date style.
     */
    HH_MM("HH:mm", true),

    /**
     * Hh mm ss date style.
     */
    HH_MM_SS("HH:mm:ss", true),

    /**
     * Mm dd en date style.
     */
    MM_DD_EN("MM/dd", true),

    /**
     * The Mm dd hh mm en.
     */
    MM_DD_HH_MM_EN("MM/dd HH:mm", true),

    /**
     * The Mm dd hh mm ss en.
     */
    MM_DD_HH_MM_SS_EN("MM/dd HH:mm:ss", true),

    /**
     * Mm dd cn date style.
     */
    MM_DD_CN("MM月dd日", true),

    /**
     * Mm dd cn 2 date style.
     */
    MM_DD_CN2("MM月dd号", true),

    /**
     * Mm dd hh mm cn date style.
     */
    MM_DD_HH_MM_CN("MM月dd日 HH:mm", true),

    /**
     * Mm dd hh mm cn 2 date style.
     */
    MM_DD_HH_MM_CN_2("MM月dd日 HH点mm分", true),

    /**
     * Mm dd hh mm cn 3 date style.
     */
    MM_DD_HH_MM_CN_3("MM月dd日 HH时mm分", true),

    /**
     * Mm dd hh mm cn 2 date style.
     */
    MM_DD_HH_MM_CN2("MM月dd号 HH:mm", true),

    /**
     * Mm dd hh mm cn 2 2 date style.
     */
    MM_DD_HH_MM_CN2_2("MM月dd号 HH点mm分", true),

    /**
     * Mm dd hh mm cn 2 3 date style.
     */
    MM_DD_HH_MM_CN2_3("MM月dd号 HH时mm分", true),

    /**
     * Mm dd hh mm ss cn date style.
     */
    MM_DD_HH_MM_SS_CN("MM月dd日 HH:mm:ss", true),

    /**
     * Mm dd hh mm ss cn 2 date style.
     */
    MM_DD_HH_MM_SS_CN_2("MM月dd日 HH点mm分ss秒", true),

    /**
     * Mm dd hh mm ss cn 3 date style.
     */
    MM_DD_HH_MM_SS_CN_3("MM月dd日 HH时mm分ss秒", true),

    /**
     * Mm dd hh mm ss cn 2 date style.
     */
    MM_DD_HH_MM_SS_CN2("MM月dd号 HH:mm:ss", true),

    /**
     * Mm dd hh mm ss cn 2 2 date style.
     */
    MM_DD_HH_MM_SS_CN2_2("MM月dd号 HH点mm分ss秒", true),

    /**
     * Mm dd hh mm ss cn 2 3 date style.
     */
    MM_DD_HH_MM_SS_CN2_3("MM月dd号 HH时mm分ss秒", true);

    private final String value;

    private final boolean isShowOnly;

    DateStyle(String value, boolean isShowOnly) {
        this.value = value;
        this.isShowOnly = isShowOnly;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Is show only boolean.
     *
     * @return the boolean
     */
    public boolean isShowOnly() {
        return isShowOnly;
    }
}
