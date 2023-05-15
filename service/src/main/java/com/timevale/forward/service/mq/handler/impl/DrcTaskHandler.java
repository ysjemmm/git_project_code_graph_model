package com.timevale.forward.service.mq.handler.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.model.enums.DrcActionEnum;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.mq.dto.MilestoneDTO;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2023/05/15 18:17
 */
@Slf4j
@LogPoint
@Component
@AllArgsConstructor
public class DrcTaskHandler {
    private final DrcProjectHandler projectHandler;

    public void handle(DrcMsgBody body) {
        riskHandle(body);
    }

    private void riskHandle(DrcMsgBody body) {
        // 新增、删除无需处理
        if (ObjectUtil.equal(DrcActionEnum.INSERT.toString(), body.getAction())
                ||ObjectUtil.equal(DrcActionEnum.DELETE.toString(), body.getAction())) {
            return;
        }

        TaskDO taskDO = JSON.parseObject(body.getAfter(), TaskDO.class);
        MilestoneDTO milestoneDTO = ProjectMilestoneCopier.INSTANCE.task2dto(taskDO);
        projectHandler.solveRisk(milestoneDTO);
    }
}
