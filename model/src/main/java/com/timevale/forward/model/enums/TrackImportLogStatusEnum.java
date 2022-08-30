package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
@AllArgsConstructor
public enum TrackImportLogStatusEnum {

    SUCCESS(0,"处理成功" ),

    FAILURE(1,"处理失败");


    final private Integer code;
    final private String text;

    public static String getTextByCode(Integer code){
        for (TrackImportLogStatusEnum e : TrackImportLogStatusEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
