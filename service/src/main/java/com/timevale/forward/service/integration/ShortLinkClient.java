package com.timevale.forward.service.integration;

import com.timevale.shortlink.common.service.api.ShortLinkRpcService;
import com.timevale.shortlink.common.service.request.ShortenRequest;
import com.timevale.shortlink.common.service.result.ShortlinkResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @auther: yuhua
 * @date: 2025/7/29 13:57
 * @description:
 */
@Component
@RequiredArgsConstructor
public class ShortLinkClient {

    private final ShortLinkRpcService shortLinkRpcService;

    // 从系统属性读取过期时间，默认3天，按秒为单位，3 * 24 * 60 * 60 = 2592000
    @Value("${token.expire.seconds: 2592000}")
    private Long expireSeconds;

    public ShortlinkResult getShortUrl(String longUrl) {
        // 生成短链接
        ShortenRequest shortenRequest = new ShortenRequest();
        shortenRequest.setUrl(longUrl);
        shortenRequest.setExpire(expireSeconds);
        return shortLinkRpcService.getShortLink(shortenRequest);
    }
}
