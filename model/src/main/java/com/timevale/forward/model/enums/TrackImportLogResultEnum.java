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

    FAILURE(1,"导入失败");


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
