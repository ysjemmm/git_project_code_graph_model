package com.timevale.forward.service.utils.date;

/**
 * @author yuankai
 * @date 2020/11/25 10:24
 */
public enum Week {
    /**
     * Monday week.
     */
    MONDAY("星期一", "Monday", "Mon.", 1),

    /**
     * Tuesday week.
     */
    TUESDAY("星期二", "Tuesday", "Tues.", 2),

    /**
     * Wednesday week.
     */
    WEDNESDAY("星期三", "Wednesday", "Wed.", 3),

    /**
     * Thursday week.
     */
    THURSDAY("星期四", "Thursday", "Thur.", 4),

    /**
     * Friday week.
     */
    FRIDAY("星期五", "Friday", "Fri.", 5),

    /**
     * Saturday week.
     */
    SATURDAY("星期六", "Saturday", "Sat.", 6),

    /**
     * Sunday week.
     */
    SUNDAY("星期日", "Sunday", "Sun.", 7);

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
     * Number week.
     */
    int number;

    /**
     * @param cn      中文名
     * @param en      英文名
     * @param enShort 英文缩写
     * @param number  数字
     */
    Week(String cn, String en, String enShort, int number) {
        this.cn = cn;
        this.en = en;
        this.enShort = enShort;
        this.number = number;
    }

    /**
     * 根据星期中文名得到星期,无匹配数据返回null
     *
     * @param cn 星期中文名
     * @return 指定的星期 week by cn
     */
    public static Week getWeekByCn(String cn) {
        for (Week week : Week.values()) {
            if (week.getChineseName().equals(cn)) {
                return week;
            }
        }
        return null;
    }

    /**
     * 根据星期英文名(忽略大小写)得到星期,无匹配数据返回null
     *
     * @param en 星期英文名
     * @return 指定的星期 week by en
     */
    public static Week getWeekByEn(String en) {
        for (Week week : Week.values()) {
            if (week.getName().equalsIgnoreCase(en)) {
                return week;
            }
        }
        return null;
    }

    /**
     * 根据星期英文缩写名(忽略大小写)得到星期,无匹配数据返回null
     *
     * @param enShort 星期英文缩写名
     * @return 指定的星期 week by short
     */
    public static Week getWeekByShort(String enShort) {
        for (Week week : Week.values()) {
            if (week.getShortName().equalsIgnoreCase(enShort)) {
                return week;
            }
        }
        return null;
    }

    /**
     * 根据星期数字得到星期,无匹配数据返回null
     *
     * @param number 星期数字
     * @return 指定的星期 week by num
     */
    public static Week getWeekByNum(int number) {
        for (Week week : Week.values()) {
            if (week.getNumber() == number) {
                return week;
            }
        }
        return null;
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
