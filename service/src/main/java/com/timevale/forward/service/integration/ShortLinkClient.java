package com.timevale.forward.service.integration;

import com.timevale.shortlink.common.service.api.ShortLinkRpcService;
import com.timevale.shortlink.common.service.request.ShortenRequest;
import com.timevale.shortlink.common.service.result.ShortlinkResult;
import lombok.RequiredArgsConstructor;
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

    public ShortlinkResult getShortUrl(String longUrl) {
        // 生成短链接
        ShortenRequest shortenRequest = new ShortenRequest();
        shortenRequest.setUrl(longUrl);
        return shortLinkRpcService.getShortLink(shortenRequest);
    }
}
