package com.timevale.forward.service.job;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/03/19 11:05
 */
@JobHandler(value = "BizDemandAutoConfirmJob")
@Slf4j
public class BizDemandAutoConfirmJob extends IJobHandler {

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        // 查询状态=待确认业务需求
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.selectByStatus(Lists.newArrayList(BizDemandStatusEnum.TO_CONFIRM.getCode()));

        // 查询对应最后一次状态变更日志
        List<Long> bizDemandIdList = bizDemandDOList.stream().map(BaseDO::getId).collect(Collectors.toList());
        List<BizChangeLogDO> bizChangeLogDOList = bizChangeLogMapper.listAllByActions(bizDemandIdList,
                BizChangeLogTypeEnum.BIZ_DEMAND.getCode(),
                Lists.newArrayList(ButtonActionEnum.COMPLETED_NOT_DEV.getText()));

        // 找出超出7天的
        Date today = new Date();
        List<Long> autoConfirmIdList = bizChangeLogDOList.stream()
                .filter(e -> DateUtil.getIntervalDays(today, e.getCreateDate()) >= 7)
                .map(BizChangeLogDO::getMainId)
                .collect(Collectors.toList());



        return ReturnT.SUCCESS;
    }
}
