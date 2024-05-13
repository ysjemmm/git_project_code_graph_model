package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.model.enums.BugFieldEnum;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.service.component.BugOfflineComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
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
    private BugLogMapper bugLogMapper;
    @Resource
    private BugOfflineMapper bugOfflineMapper;

    @Override
    public void transferOperator(Collection<Long> ids, String operator, String operatorId) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }

        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.getByIds(ids, false);
        List<BugLogDO> bugLogList = bugOfflineDOList.stream()
                .map(e -> {
                    BugLogDO bugLogDO = new BugLogDO();
                    bugLogDO.setMainId(e.getId());
                    bugLogDO.setNewValue(operator);
                    bugLogDO.setOldValue(e.getOperator());
                    bugLogDO.setField(BugFieldEnum.OPERATOR.getText());
                    bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
                    return bugLogDO;
                })
                .collect(Collectors.toList());

        bugLogMapper.batchInsert(bugLogList);
        bugOfflineMapper.updateOperator(ids, operator, operatorId);
    }

    @Override
    public void transferProposer(Collection<Long> ids, String proposer, String proposerId) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }

        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.getByIds(ids, false);
        List<BugLogDO> bugLogList = bugOfflineDOList.stream()
                .map(e -> {
                    BugLogDO bugLogDO = new BugLogDO();
                    bugLogDO.setMainId(e.getId());
                    bugLogDO.setNewValue(proposer);
                    bugLogDO.setOldValue(e.getProposer());
                    bugLogDO.setField(BugFieldEnum.PROPOSER.getText());
                    bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
                    return bugLogDO;
                })
                .collect(Collectors.toList());
        bugLogMapper.batchInsert(bugLogList);
        bugOfflineMapper.updateProposer(ids, proposer, proposerId);
    }
}
