package com.timevale.forward.service.mq.handler.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.BizRecordMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizRecordDO;
import com.timevale.forward.facade.api.result.BizRecordVO;
import com.timevale.forward.facade.api.result.BizStatusOperatorVO;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author by YangXu
 * @date 2023/05/15 18:17
 */
@Slf4j
@LogPoint
@Component
@AllArgsConstructor
public class DrcBizDemandHandler {
    private final BizRecordMapper bizRecordMapper;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(()-> recordStatusOperator(body));
    }

    public void recordStatusOperator(DrcMsgBody body) {
        BizDemandDO bizDO = JSON.parseObject(body.getAfter(), BizDemandDO.class);

        Date currentDate = new Date();
        String nowStatus = BizDemandStatusEnum.getTextByCode(bizDO.getStatus());

        // 当前状态记录人
        BizStatusOperatorVO operatorVO = new BizStatusOperatorVO(bizDO.getReceiveMan(), currentDate);

        // 获取最后的记录
        BizRecordDO lastRecordDO = bizRecordMapper.getLast(bizDO.getId(), BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
        if (lastRecordDO != null) {
            BizRecordVO lastRecordVO = JSON.parseObject(lastRecordDO.getRecord(), BizRecordVO.class);
            List<BizStatusOperatorVO> operatorVOList = lastRecordVO.getStatusOperatorVOList();
            BizStatusOperatorVO lastOperatorVO = CollUtil.getLast(operatorVOList);

            // 如果当前的状态和记录的状态一致, 且操作人不同，多增加一个操作人
            if (Objects.equals(nowStatus,lastRecordVO.getStatus())
                    && !Objects.equals(bizDO.getReceiveMan(), lastOperatorVO.getOperatorName())) {
                operatorVOList.add(operatorVO);
                operatorVOList.sort(Comparator.comparing(BizStatusOperatorVO::getOperatorDate).reversed());
                bizRecordMapper.updateRecord(lastRecordDO.getId(), JSON.toJSONString(lastRecordVO));
                return;
            }
        }

        // 新增记录
        BizRecordVO recordVO = new BizRecordVO();
        recordVO.setStatus(nowStatus);
        recordVO.setCreateDate(currentDate);
        recordVO.setStatusOperatorVOList(CollUtil.newArrayList(operatorVO));

        BizRecordDO recordDO = new BizRecordDO();
        recordDO.setMainId(bizDO.getId());
        recordDO.setMainType(BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
        recordDO.setRecord(JSON.toJSONString(recordVO));

        bizRecordMapper.insert(recordDO);
    }
}
