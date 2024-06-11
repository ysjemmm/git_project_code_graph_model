package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;


/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
@AllArgsConstructor
public enum ProjectAcceptanceRequestEnum {
    NO_NEED_ACCEPTANCE(0,"无需验收"),
    NO_ACCEPTANCE_INITIATED(1,"未发起验收"),
    ACCEPTANCE_INITIATED(2,"已发起验收"),
    ACCEPTANCE_COMPLETED(3,"验收完毕");

    final private Integer code;
    final private String text;

    public static ProjectAcceptanceRequestEnum getByCode(Integer code){
        for (ProjectAcceptanceRequestEnum e : ProjectAcceptanceRequestEnum.values()){
            if(Objects.equals(e.code, code)){
                return e;
            }
        }
        return null;
    }

}
