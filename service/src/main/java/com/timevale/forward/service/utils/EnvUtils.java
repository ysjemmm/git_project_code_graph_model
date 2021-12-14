package com.timevale.forward.service.utils;

import com.timevale.crm.sdk.common.constant.enums.EnvEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author by shanluo
 * @Date 2021/10/22 17:38
 */
@Component
public class EnvUtils {

    @Value("${spring.profiles.active}")
    private String activeProfile;

    public EnvEnum getEnv(){
        EnvEnum envEnum;
        if (activeProfile.toLowerCase().contains("prod")) {
            envEnum = EnvEnum.PROD;
        } else if (activeProfile.toLowerCase().contains("pre")) {
            envEnum = EnvEnum.PRE;
        } else {
            envEnum = EnvEnum.TEST;
        }
        return envEnum;
    }
}
