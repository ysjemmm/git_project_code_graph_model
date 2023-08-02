package com.timevale.forward.model.dto;

import com.timevale.forward.facade.api.result.enums.ModifyCheckTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author jingchun
 * create on 8/1/2023
 **/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModifyProjectCheckDTO {

    private ModifyCheckTypeEnum type;

    private boolean critical;

    private String msg;

    /**
     * 默认非critical
     */
    public ModifyProjectCheckDTO(ModifyCheckTypeEnum type, String msg) {
        this.type = type;
        this.msg = msg;
    }

}
