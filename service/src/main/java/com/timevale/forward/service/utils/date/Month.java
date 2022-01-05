package com.timevale.forward.service.utils.date;

/**
 * @author yuankai
 * @date 2020/11/25 10:23
 */
public enum Month {
    /**
     * January month.
     */
    JANUARY("一月", "January", "Jan.", 1),

    /**
     * February month.
     */
    FEBRUARY("二月", "February", "Feb.", 2),

    /**
     * March month.
     */
    MARCH("三月", "March", "Mar.", 3),

    /**
     * April month.
     */
    APRIL("四月", "April", "Apr.", 4),

    /**
     * May month.
     */
    MAY("五月", "May", "May.", 5),

    /**
     * June month.
     */
    JUNE("六月", "June", "Jun.", 6),

    /**
     * July month.
     */
    JULY("七月", "July", "Jul.", 7),

    /**
     * August month.
     */
    AUGUST("八月", "August", "Aug.", 8),

    /**
     * September month.
     */
    SEPTEMBER("九月", "September", "Sep.", 9),

    /**
     * October month.
     */
    OCTOBER("十月", "October", "Oct.", 10),

    /**
     * November month.
     */
    NOVEMBER("十一月", "November", "Nov.", 11),

    /**
     * December month.
     */
    DECEMBER("十二月", "December", "Dec.", 12);

    /**
     * The Cn.
     */
    String cn;
    /**
     * The En.
     */
    String en;
    /**
     * The En short.
     */
    String enShort;
    /**
     * The Number.
     */
    int number;

    /**
     * 根据月份中文名得到月份,无匹配数据返回null
     *
     * @param cn 月份中文名
     * @return 指定的月份 month by cn
     */
    public static Month getMonthByCn(String cn) {
        for (Month month : Month.values()) {
            if (month.getChineseName().equals(cn)) {
                return month;
            }
        }
        return null;
    }

    /**
     * 根据月份英文名(忽略大小写)得到月份,无匹配数据返回null
     *
     * @param en 月份英文文名
     * @return 指定的月份 month by en
     */
    public static Month getMonthByEn(String en) {
        for (Month month : Month.values()) {
            if (month.getName().equalsIgnoreCase(en)) {
                return month;
            }
        }
        return null;
    }

    /**
     * 根据月份英文缩写名(忽略大小写)得到月份,无匹配数据返回null
     *
     * @param enShort 月份英文缩写文名
     * @return 指定的月份 month by short
     */
    public static Month getMonthByShort(String enShort) {
        for (Month month : Month.values()) {
            if (month.getShortName().equalsIgnoreCase(enShort)) {
                return month;
            }
        }
        return null;
    }

    /**
     * 根据月份数字得到月份
     *
     * @param number 月份数字,无匹配数据返回null
     * @return 指定的月份 month by num
     */
    public static Month getMonthByNum(int number) {
        for (Month month : Month.values()) {
            if (month.getNumber() == number) {
                return month;
            }
        }
        return null;
    }

    /**
     * @param cn      中文名
     * @param en      英文名
     * @param enShort 英文缩写
     * @param number  数字
     */
    Month(String cn, String en, String enShort, int number) {
        this.cn = cn;
        this.en = en;
        this.enShort = enShort;
        this.number = number;
    }

    /**
     * 中文名
     *
     * @return chinese name
     */
    public String getChineseName() {
        return cn;
    }

    /**
     * 英文名
     *
     * @return name
     */
    public String getName() {
        return en;
    }

    /**
     * 英文缩写
     *
     * @return short name
     */
    public String getShortName() {
        return enShort;
    }

    /**
     * 数字
     *
     * @return number
     */
    public int getNumber() {
        return number;
    }
}
