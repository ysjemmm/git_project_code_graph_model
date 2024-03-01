package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.service.component.LabelComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
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
    public List<Long> getLabelIds(List<Long> labelIds, List<Long> labelCategoryIds) {
        if (CollUtil.isEmpty(labelIds) && CollUtil.isEmpty(labelCategoryIds)) {
            return Collections.emptyList();
        }

        HashSet<Long> labelIdSet = new HashSet<>();
        if (CollUtil.isNotEmpty(labelIds)) {
            labelIdSet.addAll(labelIds);
        }

        Set<Long> ofCategoryLabelIds = Collections.emptySet();
        if (CollUtil.isNotEmpty(labelCategoryIds)) {
            List<LabelDO> ofCategoryLabels = labelMapper.getByCategoryIds(labelCategoryIds, false);

            Set<Long> invalidCategoryIdSet = ofCategoryLabels.stream()
                    .filter(e -> labelIdSet.contains(e.getId()))
                    .map(LabelDO::getLabelCategoryId)
                    .collect(Collectors.toSet());

            ofCategoryLabelIds = ofCategoryLabels.stream()
                    .filter(e -> !invalidCategoryIdSet.contains(e.getLabelCategoryId()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toSet());
        }

        List<Long> result = new ArrayList<>();
        result.addAll(labelIdSet);
        result.addAll(ofCategoryLabelIds);
        return result;
    }
}
