package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.service.component.BugOnlineComponent;
import com.timevale.forward.service.utils.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class BugOnlineComponentImpl implements BugOnlineComponent {

    @Resource
    private BugOnlineMapper bugOnlineMapper;


    @Override
    public void autoCloseBugIfBeConfirm(int autoCloseLimitDay) {
        log.info("待确认线上bug自动关闭-开始");
        List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByStatus(Lists.newArrayList(BugOnlineStatusEnum.BE_CONFIRM.getCode()));
        Date today=new Date();
        List<Long> filterIds = bugOnlineDOList.stream()
                .filter(e -> DateUtil.getIntervalDays(today, e.getCreateDate()) >= autoCloseLimitDay)
                .map(BugOnlineDO::getId)
                .collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(filterIds)){
            bugOnlineMapper.updateStatusByIds(filterIds,BugOnlineStatusEnum.CLOSE.getCode());
        }
        log.info("待确认线上bug自动关闭-完成");
    }
}
