package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
@AllArgsConstructor
public enum ProjectStatusEnum {
    UNKNOWN(-1, "", false, false),
    /**
     * 0待启动,10规划中,15执行中,20研发中,25收尾中,30测试中,35运营中,40已发布,45已完成,-10已暂停,-20已作废
     */
    WAITING(0, "待启动", false, false),

    PLANING(10, "规划中", false, false),

    EXECUTING(15, "执行中", true, false),

    DEVING(20, "研发中", true, false),

    FINISHING(25, "收尾中", true, false),

    TESTING(30, "测试中", true, false),

    OPERATING(35, "运营中", true, false),

    RELEASED(40, "已发布", false, true),

    COMPLETE(45, "已完成", false, true),

    SUSPEND(-10, "已暂停", false, false),

    INVALID(-20, "已作废", false, true);

    private final Integer code;
    private final String text;
    /**
     * 是否进行中状态，非已经终止或者待开始
     */
    private final boolean ongoing;
    /**
     * 是否已经终止，已经终止的项目无法更新
     */
    private final boolean terminated;


    // 枚举项较多，初始化时放入map提升性能
    private static final Map<Integer, ProjectStatusEnum> map;
    static {
        map = Arrays.stream(values()).collect(Collectors.toMap(ProjectStatusEnum::getCode, Function.identity()));
    }

    public static ProjectStatusEnum getByCode(Integer code) {
        ProjectStatusEnum res = map.get(code);
        if (res == null) {
            return UNKNOWN;
        }
        return res;
    }

    public static String getTextByCode(Integer code) {
        return getByCode(code).getText();
    }

    public static boolean ongoing(Integer code) {
        return getByCode(code).ongoing;
    }

    public static boolean suspendOrTerminated(Integer code) {
        return code.equals(SUSPEND.code) || getByCode(code).terminated;
    }

    public static boolean terminated(Integer code) {
        return getByCode(code).terminated;
    }

}
