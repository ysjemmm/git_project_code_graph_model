package com.timevale.forward.model.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;


/**
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommonIdInput<T> {
    @NotNull(message = "id必填")
    T id;
}
