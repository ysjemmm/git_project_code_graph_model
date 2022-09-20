package com.timevale.forward.service.job;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.BugOnlineComponent;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import com.timevale.mandarin.base.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    @Resource
    private BizDemandLogComponent bizDemandLogComponent;

    @Value("${autoConfirmLimitDay:5}")
    private Integer autoConfirmLimitDay;

    @Resource
    private BugOnlineComponent bugOnlineComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> execute(String s) throws Exception {
        log.info("[BizDemandAutoConfirmJob]业务需求更新待确认-开始");

        // 查询状态=待确认业务需求
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.selectByStatus(Lists.newArrayList(BizDemandStatusEnum.TO_CONFIRM.getCode()));

        // 查询对应最后一次状态变更日志
        List<Long> bizDemandIdList = bizDemandDOList.stream().map(BaseDO::getId).collect(Collectors.toList());
        List<BizChangeLogDO> bizChangeLogDOList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(bizDemandIdList)){
            bizChangeLogDOList = bizChangeLogMapper.listAllByActions(bizDemandIdList,
                    BizChangeLogTypeEnum.BIZ_DEMAND.getCode(),
                    Lists.newArrayList(ButtonActionEnum.COMPLETED_NOT_DEV.getText()));

            // 分类取最后一条
            Map<String, BizChangeLogDO> bizChangeLogDOMap = bizChangeLogDOList.stream()
                    .collect(Collectors.toMap(
                            e -> e.getMainId() + "-" + e.getType(),
                            Function.identity(),
                            (a, b) -> a.getCreateDate().compareTo(b.getCreateDate()) >= 0 ? a : b));
            bizChangeLogDOList = new ArrayList<>(bizChangeLogDOMap.values());
        }

        // 找出超出自动确认时间的，默认为7天
        Date today = new Date();
        List<Long> autoConfirmIdList = bizChangeLogDOList.stream()
                .filter(e -> DateUtil.getIntervalDays(today, e.getCreateDate()) >= autoConfirmLimitDay)
                .map(BizChangeLogDO::getMainId)
                .collect(Collectors.toList());

        if(CollectionUtils.isNotEmpty(autoConfirmIdList)){
            // 更新状态
            bizDemandMapper.updateByIds(autoConfirmIdList, BizDemandStatusEnum.COMPLETED.getCode(), false);

            // 日志记录
            List<BizChangeLogDO> logDOList = new ArrayList<>(autoConfirmIdList.size());
            for (Long id : autoConfirmIdList) {
                BizChangeLogDO logDO = bizDemandLogComponent.getLogWhenModifyData(
                        BizDemandStatusEnum.TO_CONFIRM.getText(),
                        BizDemandStatusEnum.COMPLETED.getText(),
                        id,
                        BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                        false,
                        ButtonActionEnum.AGREE.getText()
                );
                logDOList.add(logDO);
            }
            bizChangeLogMapper.batchInsert(logDOList);
        }

        bugOnlineComponent.autoCloseBugIfBeConfirm(autoConfirmLimitDay);

        log.info("[BizDemandAutoConfirmJob]业务需求更新待确认-完成");
        return ReturnT.SUCCESS;
    }
}
