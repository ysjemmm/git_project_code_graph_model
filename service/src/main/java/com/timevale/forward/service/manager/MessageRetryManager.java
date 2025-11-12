package com.timevale.forward.service.manager;

import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageRetryManager {

    private final ErpMessageClient erpMessageClient;

    private static final int MAX_RETRIES = 3;

    @Async("threadPoolTaskExecutor")
    public void sendAsyncMessage(String jobName, ActionCardMsg actionCardMsg, String userId, AtomicInteger sentCount) {
        // 尝试发送消息，最多重试maxRetries次
        boolean success = false;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                erpMessageClient.sendActionCardMsg(actionCardMsg);
                sentCount.getAndIncrement();
                success = true;
                break;
            } catch (BaseBizRuntimeException e) {
                log.warn("[{}]第 {} 次发送失败，用户ID: {}", jobName, attempt, userId, e);
                if (attempt < MAX_RETRIES) {
                    try {
                        // 等待3秒后重试
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // 如果消息发送最终失败，则记录错误日志
        if (!success) {
            log.error("[{}]消息发送最终失败，用户ID: {}", jobName, userId);
        }

        // 执行结束时的日志记录，包括发送的消息数量
        log.info("[{}]执行完成，共发送 {} 条消息", jobName, sentCount);
    }
}