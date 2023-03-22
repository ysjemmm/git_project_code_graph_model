package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * @author by YangXu
 * @date 2022/06/24 09:52
 */
@Getter
@Setter
@ApiModel("批量请求")
public class CommonListInput<T> extends ToString {

    @ApiModelProperty("批量请求")
    private Collection<T> list;

}
