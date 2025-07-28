package com.timevale.forward.service.utils;

import com.timevale.framework.tedis.util.TedisUtil;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class UrlGenerateUtil {

    // 短链接字符集（数字+大小写字母）
    private static final String CHARS = "0123456789#abcdefghilkmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // 短链接长度
    private static final int SHORT_URL_LENGTH = 8;

    // redis key前缀
    private static final String URL_KEY_PREFIX = "FORWARD:SHORT_URL:";

    // redis key前缀
    private static final String USER_KEY_PREFIX = "FORWARD:USER_TOKEN:";

    // 3天
    private static final int EXPIRE_SECONDS = 3600 * 24 * 3;

    /**
     * 生成短链接代码
     * @param longUrl 原始链接
     * @return 短链接代码
     */
    public static String generateShortUrl(String encryptedUserId, String longUrl) {
        try {
            // 使用MD5生成哈希值
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(longUrl.getBytes(StandardCharsets.UTF_8));

            // 检查边界条件，防止数组越界
            int length = Math.min(SHORT_URL_LENGTH, hash.length);

            // 将哈希值转换为短链接
            StringBuilder shortUrl = new StringBuilder();
            for (int i = 0; i < length; i++) {
                int index = Math.abs(hash[i] % CHARS.length());
                shortUrl.append(CHARS.charAt(index));
            }

            String shortUrlCode = shortUrl.toString();
            // 增加加密用户id，防止重复短链接覆盖别人的长链接
            String redisKey = URL_KEY_PREFIX + TokenUtil.decrypt(encryptedUserId) + ":" + shortUrlCode;

            try {
                // 检查Redis中是否已存在该短链接
                String existingLongUrl = TedisUtil.get(redisKey);
                if (existingLongUrl == null) {
                    // 如果不存在，则存储新映射关系
                    TedisUtil.set(redisKey, longUrl, EXPIRE_SECONDS, TimeUnit.SECONDS);
                } else if (!longUrl.equals(existingLongUrl)) {
                    // 兜底策略，返回覆盖的短链接
                    TedisUtil.set(redisKey, longUrl, EXPIRE_SECONDS, TimeUnit.SECONDS);
                }
                // 如果已存在且URL相同，直接返回现有短链接
            } catch (Exception e) {
                throw new RuntimeException("存储短链接失败", e);
            }

            return encryptedUserId + shortUrlCode;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    public static String getUrl(String principalId, String token, StringBuilder urlBuilder, String dateStr) {
        String encryptedUserId = TokenUtil.encrypt(principalId);
        if (StringUtils.isNotBlank(token)) {
            urlBuilder.append("&userId=").append(encryptedUserId);
            // 存回redis并设置过期
            TedisUtil.set(USER_KEY_PREFIX + dateStr + ":" + principalId, token, EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
        String fullUrl = urlBuilder.toString();

        //生成短链接
        return UrlGenerateUtil.generateShortUrl(encryptedUserId, fullUrl);
    }
}
