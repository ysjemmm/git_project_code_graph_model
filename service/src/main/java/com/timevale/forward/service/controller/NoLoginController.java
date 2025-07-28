package com.timevale.forward.service.controller;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.NoLoginService;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Resource;

/**
 * @auther: yuhua
 * @date: 2025/7/27 17:02
 * @description:
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class NoLoginController {

    @Resource
    private NoLoginService noLoginService;

    @GetMapping("/getToken")
    public BaseResult<String> getToken(
            @ApiParam(value = "日期") @RequestParam(value = "dateStr") String dateStr,
            @ApiParam(value = "用户id") @RequestParam(value = "userId") String userId) {
        return noLoginService.getToken(dateStr, userId);
    }

    @GetMapping("/{url}")
    public RedirectView redirectUrl(
            @ApiParam(value = "短url") @PathVariable String url) {
        return new RedirectView(noLoginService.redirectUrl(url));
    }
}
