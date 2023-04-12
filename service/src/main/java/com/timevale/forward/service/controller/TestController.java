package com.timevale.forward.service.controller;

import cn.hutool.core.bean.BeanUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.epeius.model.ConclusionVar;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author by YangXu
 * @date 2023/03/28 16:40
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {
    private final EpeiusClient epeiusClient;

    @GetMapping("/flow")
    public BaseResult<Void> test() {
        String str = "d2276cfe-cd45-11ed-922b-5aecfee89704";
        ProcessResponse processInfo = epeiusClient.getProcessInfo(str);
        Map<String, Object> flowData = processInfo.getFlowData();
        ConclusionVar conclusionVar = BeanUtil.toBean(flowData, ConclusionVar.class);
        return BaseResult.success();
    }
}
