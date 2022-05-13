package com.timevale.forward.service.integration.epeius.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.epeius.service.api.FlowService;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.epeius.service.model.response.ProcessLogResponse;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2022/5/12 10:57
 */
@Slf4j
@Component
public class EpeiusClientImpl implements EpeiusClient {

    @Resource
    private FlowService flowService;

    @Override
    public String start(StartProcessRequest start) {
        try {
            log.info("发起工作流 start: {}", JSON.toJSONString(start));
            BaseResult<String> result = flowService.start(start);
            if (result == null || !result.ifSuccess() || result.getData() == null) {
                log.error("发起工作流失败 result: {}", result);
                throw new BaseBizRuntimeException("发起工作流失败");
            }
            log.info("发起工作流 result: {}", result.getData());
            return result.getData();
        } catch (Exception e) {
            log.warn("发起工作流异常: ", e);
            throw new BaseBizRuntimeException("发起工作流异常");
        }
    }

    @Override
    public List<ProcessLogResponse> flowLog(String processInstanceId) {
        try {
            log.info("查询工作流日志 start: {}", JSON.toJSONString(processInstanceId));
            BaseResult<List<ProcessLogResponse>> result = flowService.processLog(processInstanceId);
            if (result == null || !result.ifSuccess() || result.getData() == null) {
                log.error("查询工作流日志失败 result: {}", result);
                throw new BaseBizRuntimeException("查询工作流日志失败");
            }
            log.info("查询工作流日志 result: {}", result.getData());
            return result.getData();
        } catch (Exception e) {
            log.warn("查询工作流日志失败: ", e);
            throw new BaseBizRuntimeException("查询工作流日志失败");
        }
    }
}
