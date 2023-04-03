package com.timevale.forward.service.utils.aop;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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

    @Around("logPoint()")
    public Object doBefore(ProceedingJoinPoint joinPoint) throws Throwable {
        // 打印入参
        Signature signature = joinPoint.getSignature();
        log.info("[method]: {}.{} [input]: {}", signature.getDeclaringType(), signature.getName(), JSON.toJSON(joinPoint.getArgs()));

        try {
            // 执行方法，打印出参
            Object result = joinPoint.proceed();
            log.info("[method]: {}.{} [output]: {}", signature.getDeclaringType(), signature.getName(), JSON.toJSON(result));

            return result;
        } catch(Throwable e) {
            // 打印异常信息
            log.info("[method]: {}.{} [exception]: {}", signature.getDeclaringType(), signature.getName(), e.getMessage());
            throw e;
        }
    }
}
