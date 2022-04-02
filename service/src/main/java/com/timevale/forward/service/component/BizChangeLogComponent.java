package com.timevale.forward.service.component;

public interface BizChangeLogComponent<T> {

    void addLogWhenModifyData(T oldObj, T newObj) ;
}
