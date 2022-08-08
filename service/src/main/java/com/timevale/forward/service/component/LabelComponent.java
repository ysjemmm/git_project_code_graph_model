package com.timevale.forward.service.component;

import com.timevale.forward.facade.api.query.LabelMarkedQueryList;

import java.util.List;

public interface LabelComponent {
    /**
     *
     * @param labelMarkedQuery labelMarkedQuery
     * @return 标签id
     */
    List<Long> getLabelIds(List<LabelMarkedQueryList> labelMarkedQuery);

}
