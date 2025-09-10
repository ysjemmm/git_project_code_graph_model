package com.timevale.forward.model.enums;

import com.google.common.collect.HashBasedTable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/06/12 11:19
 */
@Getter
@AllArgsConstructor
public enum CustomerGradeEnum {
    S(10, "S", 30),
    A(20, "A", 20),
    B(30, "B", 15),
    C(40, "C", 10),
    D(45, "D", 7),
    OTHER(50, "", 5);

    private final Integer code;
    private final String text;
    private final Integer score;

    public static CustomerGradeEnum getByCode(Integer code) {
        for (CustomerGradeEnum value : CustomerGradeEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static CustomerGradeEnum getByText(String text) {
        for (CustomerGradeEnum value : CustomerGradeEnum.values()) {
            if (value.text.equals(text)) {
                return value;
            }
        }
        return null;
    }

    private static HashBasedTable<Long, Integer, Integer> scoreTable = HashBasedTable.create();
    public static void setScoreTable(HashBasedTable<Long, Integer, Integer> scoreTable) {
        CustomerGradeEnum.scoreTable = scoreTable;
    }
    public static Integer getScore(Long bizDomainId, Integer code) {
        Integer score = scoreTable.get(bizDomainId, code);
        return Optional.ofNullable(score)
                .orElse(Optional.ofNullable(code)
                        .map(CustomerGradeEnum::getByCode)
                        .map(CustomerGradeEnum::getScore)
                        .orElse(null));
    }
}
