package com.timevale.forward.service.component;

import java.util.List;

public interface LabelComponent {
    /**
     *
     * @param labelIds labelIds
     * @param labelCategoryIds labelCategoryIds
     * @return标签id
     */
    List<Long> getLabelIds(List<Long> labelIds,List<Long> labelCategoryIds);

}
