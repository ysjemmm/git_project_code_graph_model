package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOnlineStatusOperatorMapper;
import com.timevale.forward.dal.entity.BugOnlineStatusOperatorDO;
import com.timevale.forward.service.component.BugOnlineStatusOperatorComponent;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yexuan
 * @date 2022-12-08 14:32 yexuan
 */
@Component
@Slf4j
public class BugOnlineStatusOperatorComponentImpl implements BugOnlineStatusOperatorComponent {

    @Resource
    private BugOnlineStatusOperatorMapper bugOnlineStatusOperatorMapper;

    @Override
    public void add(BugOnlineStatusOperatorDO bugOnlineStatusOperatorDO) {

        if(bugOnlineStatusOperatorDO.getBugOnlineId()==null || bugOnlineStatusOperatorDO.getStatus()==null){
            return;
        }
        Long id = bugOnlineStatusOperatorMapper.getByBugOnlineIdAndStatus(bugOnlineStatusOperatorDO.getBugOnlineId(),bugOnlineStatusOperatorDO.getStatus());
        if(id == null){
            bugOnlineStatusOperatorMapper.insert(bugOnlineStatusOperatorDO);
        }else {
            bugOnlineStatusOperatorMapper.updateOperatorById(bugOnlineStatusOperatorDO.getOperatorId(),bugOnlineStatusOperatorDO.getOperator(),id);
        }
    }

    @Override
    public void delete(Long bugOnlineId) {
        bugOnlineStatusOperatorMapper.deleteByBugOnlineId(bugOnlineId);
    }
}
