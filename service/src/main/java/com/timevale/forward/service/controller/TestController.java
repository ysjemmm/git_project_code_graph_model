package com.timevale.forward.service.controller;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.job.ProjectAutoAcceptanceJob;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author by YangXu
 * @date 2023/03/28 16:40
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {
    private final ProjectAutoAcceptanceJob projectAutoAcceptanceJob;

    @GetMapping("/job/accept")
    public BaseResult<Void> test() throws Exception {
        projectAutoAcceptanceJob.execute("");
        return BaseResult.success();
    }
}
