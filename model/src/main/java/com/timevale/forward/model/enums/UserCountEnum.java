package com.timevale.forward.model.enums;

import com.google.common.collect.HashBasedTable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/06/09 17:40
 */
@Getter
@AllArgsConstructor
public enum UserCountEnum {

    LEQ_TWO(1, "1~2个", 0),
    GEQ_THI(2, "3个或3个以上", 10);

    private final Integer code;
    private final String text;
    private final Integer score;

    public static UserCountEnum getByCode(Integer code) {
        for (UserCountEnum value : UserCountEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static String getTextByCode(Integer code) {
        return Optional.ofNullable(getByCode(code))
                .map(UserCountEnum::getText)
                .orElse("");
    }

    private static HashBasedTable<Long, Integer, Integer> scoreTable = HashBasedTable.create();
    public static void setScoreTable(HashBasedTable<Long, Integer, Integer> scoreTable) {
        UserCountEnum.scoreTable = scoreTable;
    }
    public static Integer getScore(Long bizDomainId, Integer code) {
        Integer score = scoreTable.get(bizDomainId, code);
        return Optional.ofNullable(score)
                .orElse(Optional.ofNullable(code)
                        .map(UserCountEnum::getByCode)
                        .map(UserCountEnum::getScore)
                        .orElse(null));
    }
}
