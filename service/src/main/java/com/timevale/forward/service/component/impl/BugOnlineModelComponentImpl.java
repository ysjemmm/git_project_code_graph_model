package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOnlineModelMapper;
import com.timevale.forward.dal.entity.BugOnlineModelDO;
import com.timevale.forward.service.component.BugOnlineModelComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Date 2022/3/22 10:32
 * @Author 望轩
 */
@Component
@Slf4j
public class BugOnlineModelComponentImpl implements BugOnlineModelComponent {


    @Resource
    private BugOnlineModelMapper bugOnlineModelMapper;

    @Override
    public void update(List<Long> modelIds, Long id) {
        BugOnlineModelDO delete = new BugOnlineModelDO();
        delete.setBugOnlineId(id);
        delete.setIsDeleted(true);
        bugOnlineModelMapper.update(delete);

        if(!CollectionUtils.isEmpty(modelIds)){
            List<BugOnlineModelDO> bugOnlineModelDOList = new ArrayList<>();
            modelIds.forEach(modelId -> {
                BugOnlineModelDO bugOnlineModelDO = new BugOnlineModelDO();
                bugOnlineModelDO.setBugOnlineId(id);
                bugOnlineModelDO.setModelId(modelId);
                bugOnlineModelDOList.add(bugOnlineModelDO);
            });
            //插入新的数据
            bugOnlineModelMapper.batchInsert(bugOnlineModelDOList);
        }
    }

    @Override
    public void add(List<Long> modelIds, Long id) {
        List<BugOnlineModelDO> bugOnlineModelDOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(modelIds)) {
            modelIds.forEach(modelId -> {
                BugOnlineModelDO bugOnlineModelDO = new BugOnlineModelDO();
                bugOnlineModelDO.setBugOnlineId(id);
                bugOnlineModelDO.setModelId(modelId);
                bugOnlineModelDOList.add(bugOnlineModelDO);
            });
            bugOnlineModelMapper.batchInsert(bugOnlineModelDOList);
        }
    }
}