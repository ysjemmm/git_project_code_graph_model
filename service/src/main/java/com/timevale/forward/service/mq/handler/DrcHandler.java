package com.timevale.forward.service.mq.handler;

import com.timevale.forward.service.mq.dto.DrcMsgBody;

/**
 * @author by YangXu
 * @date 2023/05/15 18:03
 */
@FunctionalInterface
public interface DrcHandler {
    void handle(DrcMsgBody drcMsgBody);
}
