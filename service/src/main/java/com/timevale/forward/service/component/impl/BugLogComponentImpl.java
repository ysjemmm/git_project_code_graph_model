package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.dao.BugLogMapper;
import com.timevale.forward.dal.dao.BugStatusOperatorMapper;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugStatusOperatorDO;
import com.timevale.forward.model.enums.BugLogFieldEnum;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.service.component.BugLogComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class BugLogComponentImpl implements BugLogComponent {


    @Resource
    private BugLogMapper bugLogMapper;

    @Resource
    private BugStatusOperatorMapper bugStatusOperatorMapper;

    public void insertToBugStatusOperator(Long bugId, String userId, String userName,Integer bugType) {
        //查询当前线上bug对应的所有状态变更记录
        List<BugLogDO> bugLogDOS = bugLogMapper.selectByBugOfflineIdAndType(bugId, bugType, true);

        //按创建时间逆序排列，筛选出最后一条状态变更记录
        List<BugLogDO> collect = bugLogDOS.stream().filter(a-> BugLogFieldEnum.STATUS.getText().equals(a.getField()))
                .sorted(Comparator.comparing(BugLogDO::getCreateDate).reversed()).collect(Collectors.toList());
        BugLogDO lastStatusBugLogDO = collect.get(0);

        BugStatusOperatorDO bugStatusOperatorDO = new BugStatusOperatorDO();
        bugStatusOperatorDO.setBugLogId(lastStatusBugLogDO.getId());
        bugStatusOperatorDO.setOperator(userName);
        bugStatusOperatorDO.setOperatorId(userId);
        //往状态人员处理表里面插入一条数据记录
        bugStatusOperatorMapper.insert(bugStatusOperatorDO);
    }

    @Override
    public void insertToBugStatusOperator(Long bugOnlineId, String userId, String userName) {
        insertToBugStatusOperator(bugOnlineId,userId,userName, BugLogTypeEnum.ONLINE.getCode());
    }

    @Override
    public void bugOnlineInit(Long id) {
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setMainId(id);
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setOldValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        bugLogDO.setNewValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        bugLogDO.setAction(ButtonActionEnum.SUBMIT.getText());
        bugLogMapper.insert(bugLogDO);
    }

    @Override
    public void add(BugLogDO bugLogDO) {
        if (bugLogDO == null) {
            return;
        }
        bugLogMapper.insert(bugLogDO);
    }

    @Override
    public void add(List<BugLogDO> logDOList) {
        if (CollUtil.isEmpty(logDOList)) {
            return;
        }
        logDOList = logDOList.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(logDOList)) {
            bugLogMapper.batchInsert(logDOList);
        }
    }
}
