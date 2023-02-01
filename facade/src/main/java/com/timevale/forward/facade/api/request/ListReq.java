package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 通用 list 类型包装
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
public class ListReq<T> extends ToString {

    private List<T> list;

}
