package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.NoLoginService;
import com.timevale.forward.service.utils.TokenUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.framework.tedis.util.TedisUtil;
import com.timevale.mandarin.base.util.StringUtils;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

/**
 * @auther: yuhua
 * @date: 2025/7/28 10:59
 * @description: 免登录服务实现
 */
@Slf4j
@LogPoint
@RestService
public class NoLoginServiceImpl implements NoLoginService {

    // redis token key前缀
    private static final String USER_KEY_PREFIX = "FORWARD:USER_TOKEN:";

    @Override
    public BaseResult<String> getToken(String dateStr, String userId) {
        if (StringUtils.isEmpty(dateStr) || StringUtils.isEmpty(userId)) {
            return BaseResult.success("");
        }

        try {
            String decrypt = TokenUtil.decrypt(userId);
            if (StringUtils.isEmpty(decrypt)) {
                return BaseResult.success("");
            }

            String redisKey = USER_KEY_PREFIX + dateStr + ":" + decrypt;
            String token = TedisUtil.get(redisKey);
            if (StringUtils.isNotBlank(token)) {
                return BaseResult.success(token);
            }
            return BaseResult.success("");
        } catch (Exception e) {
            // 记录日志
            return BaseResult.success("");
        }
    }
}
