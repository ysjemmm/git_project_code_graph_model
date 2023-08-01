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

    private String msg;

}
