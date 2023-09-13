package com.timevale.forward.service.job;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BizRecordMapper;
import com.timevale.forward.dal.dao.BizTimeMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizRecordDO;
import com.timevale.forward.dal.entity.BizTimeDO;
import com.timevale.forward.facade.api.result.BizRecordVO;
import com.timevale.forward.facade.api.result.BizStatusOperatorVO;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@JobHandler("bizTimeJob")
public class BizTimeJob extends IJobHandler {
    private final BizTimeMapper bizTimeMapper;
    private final BizDemandMapper bizDemandMapper;
    private final BizRecordMapper bizRecordMapper;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        log.info("[bizTimeJob]开始执行");

        List<BizRecordDO> recordDOList = bizRecordMapper.getByMainType(4);
        Map<Long, List<BizRecordDO>> recordGroupId = recordDOList.stream()
                .collect(Collectors.groupingBy(BizRecordDO::getMainId));

        Set<Long> bizIds = recordDOList.stream().map(BizRecordDO::getMainId).collect(Collectors.toSet());
        List<BizDemandDO> bizDemands = bizDemandMapper.getByIds(bizIds);
        Map<Long, BizDemandDO> bizMap = bizDemands.stream()
                .collect(Collectors.toMap(BaseDO::getId, Function.identity()));


        Date endDate = new Date();
        List<BizTimeDO> bizTimes = new ArrayList<>();

        recordGroupId.forEach((mainId, recordEntities) -> {
            if (!bizMap.containsKey(mainId)) {
                return;
            }

            List<BizRecordVO> recordVOList = recordEntities.stream()
                    .map(e -> JSON.parseObject(e.getRecord(), BizRecordVO.class))
                    .sorted(Comparator.comparing(BizRecordVO::getCreateDate))
                    .collect(Collectors.toList());

            Table<String, String, Long> table = HashBasedTable.create();

            Date date = null;
            String status = "";
            String operatorName = "";
            BizDemandDO bizDemand = bizMap.get(mainId);
            for (BizRecordVO record : recordVOList) {
                List<BizStatusOperatorVO> operators = record.getStatusOperatorVOList();
                operators.sort(Comparator.comparing(BizStatusOperatorVO::getOperatorDate));

                for (BizStatusOperatorVO operator : operators) {
                    if (date == null) {
                        status = record.getStatus();
                        date = operator.getOperatorDate();
                        operatorName = operator.getOperatorName();
                        continue;
                    }
                    Long interval = operator.getOperatorDate().getTime() - date.getTime();

                    Long theTimeConsumption = table.get(operatorName, status);
                    if (theTimeConsumption == null) {
                        theTimeConsumption = 0L;
                    }
                    theTimeConsumption = theTimeConsumption + interval;
                    if (ObjectUtil.hasEmpty(operatorName, status, theTimeConsumption)) {
                        log.error("[BizTimeJob]存在空记录 record:{}", JSONObject.toJSONString(record));
                    } else {
                        table.put(operatorName, status, theTimeConsumption);
                    }

                    status = record.getStatus();
                    date = operator.getOperatorDate();
                    operatorName = operator.getOperatorName();
                }
            }

            if (!(BizDemandStatusEnum.INVALID.getCode().equals(bizDemand.getStatus())
                    || BizDemandStatusEnum.AVAILABLE.getCode().equals(bizDemand.getStatus())
                    || BizDemandStatusEnum.PJ_SUSPEND.getCode().equals(bizDemand.getStatus()))) {
                if (date == null) {
                    date = endDate;
                }
                Long interval = endDate.getTime() - date.getTime();

                Long theTimeConsumption = table.get(bizDemand.getReceiveMan(), BizDemandStatusEnum.getTextByCode(bizDemand.getStatus()));
                if (theTimeConsumption == null) {
                    theTimeConsumption = 0L;
                }
                theTimeConsumption = theTimeConsumption + interval;
                table.put(bizDemand.getReceiveMan(),  BizDemandStatusEnum.getTextByCode(bizDemand.getStatus()), theTimeConsumption);
            }

            Map<String, Map<String, Long>> tableMap = table.rowMap();
            tableMap.forEach((userName, statusMap) -> {
                statusMap.forEach((bizStatus, time) -> {
                    BizTimeDO bizTime = new BizTimeDO();
                    bizTime.setMainType(4);
                    bizTime.setMainId(mainId);
                    bizTime.setStatus(bizStatus);
                    bizTime.setUserName(userName);
                    bizTime.setTime(time);
                    bizTimes.add(bizTime);
                });
            });
        });

        bizTimeMapper.deleteAll();
        bizTimeMapper.batchInsert(bizTimes);
        log.info("[bizTimeJob]执行完成");
        return ReturnT.SUCCESS;
    }
}
