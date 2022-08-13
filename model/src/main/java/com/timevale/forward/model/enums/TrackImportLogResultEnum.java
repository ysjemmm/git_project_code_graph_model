package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author by YangXu
 * @date 2022/08/12 13:52
 */
@Getter
@AllArgsConstructor
public enum TrackImportLogResultEnum {

    SUCCESS(0,"导入成功" ),

    FAILURE(1,"导入失败"),

    LOADING(2, "导入中"),

    NOTING(3,"无导入事件"),

    CANCEL(4,"取消导入"),

    ;


    final private Integer code;
    final private String text;

    public static String getTextByCode(Integer code){
        for (TrackImportLogResultEnum e : TrackImportLogResultEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
