package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.service.component.BugOfflineComponent;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
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
public class BugOfflineComponentImpl implements BugOfflineComponent {

    @Resource
    private BugOfflineMapper bugOfflineMapper;

    @Override
    public void containProductLineInBugOffline(Long projectId, List<Long> productLineIdsInProject) {
        List<Long> productLineIdsInBugOffline = bugOfflineMapper.selectByProjectId(projectId)
                .stream().map(BugOfflineDO::getProductLineId).collect(Collectors.toList());
        AssertUtil.checkState(CollUtil.containsAny(productLineIdsInBugOffline, productLineIdsInProject),
                "该产品线已关联线下bug，无法修改");
    }

}
