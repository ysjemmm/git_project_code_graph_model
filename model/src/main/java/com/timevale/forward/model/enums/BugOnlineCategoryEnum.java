package com.timevale.forward.model.enums;

import com.google.common.collect.HashBasedTable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;


/**
 * @author by YangXu
 * @date 2023/03/22 17:23
 */
@Getter
@AllArgsConstructor
public enum BugOnlineCategoryEnum {
    FUNCTION(1, "功能问题", 15),
    PERFORMANCE(2, "性能问题", 10),
    CAPABILITY(3, "兼容性问题", 10),
    UE(4, "用户体验问题", 5),
    SECURITY(5, "安全问题", 5),
    DATA(6,"数据问题",5)
    ;

    private final Integer code;
    private final String text;
    private final Integer score;

    public static String getTextByCode(Integer code) {
        for (BugOnlineCategoryEnum e : BugOnlineCategoryEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }

    public static BugOnlineCategoryEnum getByCode(Integer code) {
        for (BugOnlineCategoryEnum value : BugOnlineCategoryEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    private static HashBasedTable<Long, Integer, Integer> scoreTable = HashBasedTable.create();
    public static void setScoreTable(HashBasedTable<Long, Integer, Integer> scoreTable) {
        BugOnlineCategoryEnum.scoreTable = scoreTable;
    }
    public static Integer getScore(Long bizDomainId, Integer code) {
        Integer score = scoreTable.get(bizDomainId, code);
        return Optional.ofNullable(score)
                .orElse(Optional.ofNullable(code)
                        .map(BugOnlineCategoryEnum::getByCode)
                        .map(BugOnlineCategoryEnum::getScore)
                        .orElse(null));
    }
}
