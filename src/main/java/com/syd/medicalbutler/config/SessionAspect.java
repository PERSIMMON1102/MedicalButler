package com.syd.medicalbutler.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;


@Aspect
@Component
public class SessionAspect {


    @Pointcut("execution(* com.syd.medicalbutler.serviceImpl.*.*(..))")
    public void executeService() {}

    @Around("executeService()")
    public Object doAroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        // 读取当前请求的 HttpSession（若不存在请求上下文，RequestContextHolder.getRequestAttributes() 可能为 null）
        Object user = null;
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                HttpServletRequest request = attrs.getRequest();
                if (request != null) {
                    HttpSession session = request.getSession(false); // false: 不强制创建 session
                    if (session != null) {
                        user = session.getAttribute("user");
                    }
                }
            }
        } catch (Exception e) {
            // 不要因获取用户失败而中断业务流程，记录日志即可
            System.err.println("SessionAspect: 获取 session/user 时发生异常: " + e.getMessage());
        }

        // 设置到 ThreadLocal（如果为 null 也可以设置为 null，以便调用方统一通过 RequestContext.getCurrentUser() 读取）
        try {
            RequestContext.setCurrentUser(user);
            // 执行目标方法
            Object result = joinPoint.proceed();
            return result;
        } finally {
            // 始终清理 ThreadLocal，避免内存泄漏或线程数据穿透
            RequestContext.clear();
            long cost = System.currentTimeMillis() - start;
            // 你可以换成日志框架记录（Slf4j 等），这里只用 System.out 便于示例
            System.out.println("SessionAspect - method: " + joinPoint.getSignature() + " executed in " + cost + "ms");
        }
    }
}