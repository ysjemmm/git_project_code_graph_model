package com.timevale.forward.service.utils;

import com.timevale.framework.tedis.util.TedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TokenUtil {

    private static final String ALGORITHM = "AES";

    private static final String AES_KEY = System.getProperty("AES_KEY", "forward2025esign");

    // redis key前缀
    private static final String USER_KEY_PREFIX = "FORWARD:USER_TOKEN:";

    // 从系统属性读取过期时间，默认3天
    private static final long EXPIRE_SECONDS = Long.parseLong(
            System.getProperty("token.expire.seconds", String.valueOf(259200))
    );

    /**
     * AES加密
     */
    public static String encrypt(String token) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(), ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(token.getBytes());
            // 使用 URL 安全的 Base64 编码，避免 / 和 + 字符
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            // 加密失败时返回原始token
            return token;
        }
    }

    /**
     * AES解密
     */
    public static String decrypt(String encryptedToken) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(AES_KEY.getBytes(), ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            // 使用 URL 安全的 Base64 解码
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedToken);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            // 解密失败时返回null
            return null;
        }
    }

    public static void setTokenExpireTime(String principalId, String token, StringBuilder urlBuilder, String dateStr) {
        String encryptedUserId = TokenUtil.encrypt(principalId);
        if (StringUtils.isNotBlank(token)) {
            urlBuilder.append("&userId=").append(encryptedUserId);
            // 存回redis并设置过期
            TedisUtil.set(USER_KEY_PREFIX + dateStr + ":" + principalId, token, EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }
}
