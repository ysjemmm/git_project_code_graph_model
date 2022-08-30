package com.timevale.forward.service.utils.envoy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timevale.mandarin.base.exception.BaseRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Base64;

/**
 * session 数据相关工具
 *
 * @author jingchun
 * created on 2021/10/21
 */
@Slf4j
public class LocalSessionUtils {

    public final static String ADMIN = "admin";
    public final static String SYSTEM = "SYSTEM";
    public final static String SYSTEM_ALIAS = "系统";

    public static UserInfo getUserInfo() throws BaseRuntimeException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (attributes == null) {
            UserInfo userInfo = new UserInfo();
            userInfo.setName(SYSTEM);
            userInfo.setAlias(SYSTEM);
            userInfo.setId(SYSTEM);
            return userInfo;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String userInfoHeader = attributes.getRequest().getHeader("x-timevale-jwtcontent");
        if (StringUtils.isEmpty(userInfoHeader)) {
            UserInfo userInfo = new UserInfo();
            userInfo.setName(SYSTEM);
            userInfo.setAlias(SYSTEM);
            userInfo.setId(SYSTEM);
            return userInfo;
        }
        byte[] userInfo = Base64.getUrlDecoder().decode(userInfoHeader);
        UserInfo result;
        try {
            result = mapper.readValue(userInfo, UserInfo.class);
        } catch (IOException e) {
            log.info("userSession:{} 解析用户信息失败", new String(userInfo), e);
            throw new BaseRuntimeException("401", "登录超时", e);
        }
        if (StringUtils.isBlank(result.getId())) {
            result.setId(ADMIN);
            result.setAlias(ADMIN);
            result.setName(ADMIN);
        }
        return result;
    }

}
