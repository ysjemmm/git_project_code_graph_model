/*
 *  __  .__                            .__
 *_/  |_|__| _____   _______  _______  |  |   ____
 *\   __\  |/     \_/ __ \  \/ /\__  \ |  | _/ __ \
 * |  | |  |  Y Y  \  ___/\   /  / __ \|  |_\  ___/
 * |__| |__|__|_|  /\___  >\_/  (____  /____/\___  >
 *               \/     \/           \/          \/
 *
 *                   Copyright 2017-2017 Timevale.
 */
package com.timevale.forward.deploy;

import com.timevale.billing.log.monitor.interfaces.EnableLogMonitor;
import com.timevale.framework.puppeteer.spring.annotation.EnablePuppeteerConfig;
import com.timevale.mandarin.microservice.UniversalService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;

/**
 * 服务启动入口
 *
 * @author LIU Kunpeng  WeChat:13758206010
 * @version $Id: Application.java, v 0.1 2017年11月13日 下午2:23:03 LIU Kunpeng Exp $
 */
@UniversalService
@EnableFeignClients(basePackages = {
        "com.timevale.filesystem.common.service.api",
        "com.timevale.security.facade.api",
        "com.timevale.erp.message.service.api",
        "com.timevale.lowcode.support.api",
        "com.timevale.epeius.service.api",
        "com.timevale.crm.custom.provider.facade.api",
        "com.timevale.crm.dock.facade.api",
        "com.timevale.encourage.facade.api"
})
@MapperScan("com.timevale.forward.dal")
@SpringBootApplication(scanBasePackages = {
        "com.timevale.crm.sdk.common.base",
        "com.timevale.crm.sdk.common.utils.file",
        "com.timevale.forward.service"
})
@EnablePuppeteerConfig({"application", "JSBZ.SOA_PUBLIC"})
@EnableLogMonitor
public class Application {

    public static void main(String[] args) {SpringApplication.run(Application.class, args);}
}
