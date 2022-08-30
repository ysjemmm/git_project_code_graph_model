package com.timevale.forward.service.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2022/01/21 14:59
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    public static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    public static Object getBean(Class clazz){
        return applicationContext.getBean(clazz);
    }

    public static String getProperty(String key){
        return applicationContext.getEnvironment().getProperty(key);
    }

    public static String getProperty(String key, String defaultValue){
        String property = applicationContext.getEnvironment().getProperty(key);
        return property == null ? defaultValue : property;
    }

}
