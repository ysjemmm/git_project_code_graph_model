package com.timevale.forward.service.impl;

import com.timevale.crm.sdk.common.constant.enums.EnvEnum;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.NoLoginService;
import com.timevale.forward.model.bo.UrlComponentBO;
import com.timevale.forward.service.utils.EnvUtils;
import com.timevale.forward.service.utils.TokenUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.framework.tedis.util.TedisUtil;
import com.timevale.mandarin.base.util.StringUtils;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @auther: yuhua
 * @date: 2025/7/28 10:59
 * @description: 免登录服务实现
 */
@Slf4j
@LogPoint
@RestService
public class NoLoginServiceImpl implements NoLoginService {

    @Resource
    private EnvUtils envUtils;

    // redis token key前缀
    private static final String USER_KEY_PREFIX = "FORWARD:USER_TOKEN:";

    // redis 短链key前缀
    private static final String URL_KEY_PREFIX = "FORWARD:SHORT_URL:";

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

    @Override
    public String redirectUrl(String url) {
        // 请求参数
        log.info("重定向接收参数:{}", url);
        String baseUrl = EnvEnum.PROD.equals(envUtils.getEnv()) ? "https://forward.esign.cn" : "https://testforward.tsign.cn";
        String defaultRedirectUrl = baseUrl + "/mobileTimeRegistration";

        // 基本参数验证
        if (StringUtils.isEmpty(url)) {
            return defaultRedirectUrl;
        }

        try {
            // 解析URL参数
            UrlComponentBO urlComponents = parseUrlComponents(url);
            if (urlComponents == null) {
                return defaultRedirectUrl;
            }

            // 解密用户ID
            String decryptedUserId = decryptUserId(urlComponents.getEncryptedUserId());
            if (StringUtils.isEmpty(decryptedUserId)) {
                return defaultRedirectUrl;
            }

            // 构造Redis键并获取长链接
            String redisKey = buildRedisKey(decryptedUserId, urlComponents.getShortUrlCode());
            String longUrl = getLongUrlFromRedis(redisKey);

            // 验证并返回结果
            if (isValidUrl(longUrl)) {
                log.info("重定向返回链接:{}", longUrl);
                return longUrl;
            }

        } catch (Exception e) {
            log.warn("Error processing redirect URL: {}", url, e);
        }

        return defaultRedirectUrl;
    }

    /**
     * 解析URL组件
     */
    private UrlComponentBO parseUrlComponents(String url) {
        try {
            if (url.length() < 8) {
                log.warn("URL length is less than 8 characters: {}", url);
                return null;
            }

            String encryptedUserId = url.substring(0, url.length() - 8);
            String shortUrlCode = url.substring(url.length() - 8);

            if (StringUtils.isEmpty(encryptedUserId)) {
                log.warn("Encrypted user ID is empty");
                return null;
            }

            if (StringUtils.isEmpty(shortUrlCode)) {
                log.warn("Short URL code is empty");
                return null;
            }

            return new UrlComponentBO(encryptedUserId, shortUrlCode);
        } catch (StringIndexOutOfBoundsException e) {
            log.warn("String index out of bounds when parsing URL: {}", url, e);
            return null;
        }
    }

    /**
     * 解密用户ID
     */
    private String decryptUserId(String encryptedUserId) {
        try {
            String decryptedUserId = TokenUtil.decrypt(encryptedUserId);
            if (StringUtils.isEmpty(decryptedUserId)) {
                log.warn("Failed to decrypt user ID: {}", encryptedUserId);
            }
            return decryptedUserId;
        } catch (Exception e) {
            log.warn("Error decrypting user ID: {}", encryptedUserId, e);
            return null;
        }
    }

    /**
     * 从Redis获取长链接
     */
    private String getLongUrlFromRedis(String redisKey) {
        try {
            String longUrl = TedisUtil.get(redisKey);
            if (StringUtils.isEmpty(longUrl)) {
                log.warn("No long URL found for redis key: {}", redisKey);
            }
            return longUrl;
        } catch (Exception e) {
            log.warn("Failed to get long url from redis for key: {}", redisKey, e);
            return null;
        }
    }

    /**
     * 验证URL是否有效
     */
    private boolean isValidUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return false;
        }

        if (!url.startsWith("https://")) {
            log.warn("URL protocol not allowed: {}", url);
            return false;
        }

        return true;
    }

    /**
     * 构造Redis键
     */
    private String buildRedisKey(String userId, String shortUrlCode) {
        return URL_KEY_PREFIX + userId + ":" + shortUrlCode;
    }
}
