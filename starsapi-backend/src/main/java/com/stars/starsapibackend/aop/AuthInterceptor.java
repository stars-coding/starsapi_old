package com.stars.starsapibackend.aop;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.stars.starsapibackend.annotation.AuthCheck;
import com.stars.starsapibackend.common.ErrorCode;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.service.UserService;
import com.stars.starsapicommon.model.entity.User;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限拦截器
 * 权限校验AOP
 *
 * @author stars
 */
@Aspect
@Component
public class AuthInterceptor {

    // 依赖注入的UserService实例
    @Resource
    private UserService userService;

    /**
     * 执行拦截，对带有@AuthCheck注解的方法进行权限校验
     *
     * @param joinPoint 切点对象，包含被拦截的方法信息
     * @param authCheck AuthCheck注解对象，用于指定权限校验规则
     * @return 被拦截方法的执行结果
     * @throws Throwable 可能抛出的异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取允许的任意角色列表
        List<String> anyRole = Arrays.stream(authCheck.anyRole())
                .filter(StringUtils::isNotBlank) // 过滤掉空白的角色名称
                .collect(Collectors.toList());
        // 获取必须具备的角色名称
        String mustRole = authCheck.mustRole();
        // 获取当前请求的属性
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        // 获取HTTP请求对象
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户信息
        User user = userService.getLoginUser(request);
        // 如果允许任意角色访问
        if (CollectionUtils.isNotEmpty(anyRole)) {
            String userRole = user.getUserRole();
            if (!anyRole.contains(userRole)) {
                // 抛出无权限错误异常
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 如果必须具备特定角色才能访问
        if (StringUtils.isNotBlank(mustRole)) {
            String userRole = user.getUserRole();
            if (!mustRole.equals(userRole)) {
                // 抛出无权限错误异常
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 通过权限校验，放行，继续执行被拦截的方法
        return joinPoint.proceed();
    }
}
