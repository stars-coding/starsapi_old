package com.stars.starsapibackend.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * 日志拦截器
 * 日志AOP
 *
 * @author stars
 */
@Aspect
@Component
@Slf4j
public class LogInterceptor {

    /**
     * 执行拦截，记录请求和响应日志
     *
     * @param point 切点对象，包含被拦截的方法信息
     * @return 被拦截方法的执行结果
     * @throws Throwable 可能抛出的异常
     */
    @Around("execution(* com.stars.starsapibackend.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 获取请求路径
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 生成请求唯一id
        String requestId = UUID.randomUUID().toString();
        // 获取请求的URL路径
        String url = httpServletRequest.getRequestURI();
        // 获取请求参数
        Object[] args = point.getArgs();
        // 将参数数组转为字符串
        String reqParam = "[" + StringUtils.join(args, ", ") + "]";
        // 输出请求日志
        log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url,
                httpServletRequest.getRemoteHost(), reqParam);
        // 执行原方法
        Object result = point.proceed();
        // 输出响应日志
        // 停止计时
        stopWatch.stop();
        // 获取总耗时
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        // 输出请求结束日志
        log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);
        // 返回原方法的执行结果
        return result;
    }
}
