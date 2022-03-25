package com.timevale.forward.service.utils.aop;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2022/03/25 15:52
 */
@Aspect
@Slf4j
@Component
public class LogAspect {

    @Pointcut("@within(LogPoint)")
    public void logPoint(){
    }

    @Before("logPoint()")
    public void doBefore(JoinPoint joinPoint){
        log.info("==================== Start ====================");

        // 打印方法名
        Signature signature = joinPoint.getSignature();
        log.info("Class Method    : {}.{}", signature.getDeclaringType(), signature.getName());

        // 打印请求入参
        log.info("Request Args     : {}", JSON.toJSON(joinPoint.getArgs()));

        log.info("====================  End  ====================");
    }
}
