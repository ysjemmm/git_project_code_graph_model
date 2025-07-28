package com.timevale.forward.service.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtGeneratorUtil {
    public static String generateJwt(String userId, String alias, String userName) {
        // 构建JSON字符串
        String jsonPayload = String.format("{\"id\":\"%s\",\"alias\":\"%s\",\"name\":\"%s\"}", 
                                         userId, alias, userName);
        
        // 使用Base64 URL编码
        byte[] bytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        String jwt = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        
        return jwt;
    }
    
    public static void main(String[] args) {
        // 示例用法
        String userId = "yuhua";
        String alias = "雨桦";
        String userName = "杨军辉";
        
        String jwt = generateJwt(userId, alias, userName);
        System.out.println("Generated JWT: " + jwt);
    }
}