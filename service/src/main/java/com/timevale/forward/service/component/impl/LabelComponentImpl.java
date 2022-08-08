package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.facade.api.query.LabelMarkedQueryList;
import com.timevale.forward.service.component.LabelComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class LabelComponentImpl implements LabelComponent {


    @Resource
    private LabelMapper labelMapper;

    @Override
    public List<Long> getLabelIds(List<LabelMarkedQueryList> labelMarkedQuery) {
        List<Long> labelCategoryIds = labelMarkedQuery.stream().filter(a->a.getLabelCategoryId()!=null).map(LabelMarkedQueryList::getLabelCategoryId).collect(Collectors.toList());
        List<Long> labelIds = labelMarkedQuery.stream().filter(a->a.getLabelId()!=null).map(LabelMarkedQueryList::getLabelId).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(labelCategoryIds)){
            List<LabelDO> labelDOList = labelMapper.getByCategoryIds(labelCategoryIds);
            List<Long> oldLabelIds = labelDOList.stream().map(LabelDO::getId).collect(Collectors.toList());
            labelIds.addAll(oldLabelIds);
        }
        return labelIds;
    }
}
