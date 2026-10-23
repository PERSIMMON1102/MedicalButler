package com.syd.medicalbutler.config;

import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class LoginIntercepor implements HandlerInterceptor {
    private static final List<String> WHITELIST_PREFIX = Arrays.asList(
            "/",
            "/login.html",
            "/loginapp.html",
            "/register.html",
            "/api/user/register",
            "/User/login",
            "/layui/",
            "/static/",
            "/css/",
            "/js/",
            "/images/",
            "/pic/"
    );

    private boolean isAjax(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return (xrw != null && "XMLHttpRequest".equalsIgnoreCase(xrw))
                || (accept != null && accept.contains("application/json"));
    }

    private boolean isWhitelisted(String uri) {
        if (uri == null) return true;
        for (String p : WHITELIST_PREFIX) {
            if (p.endsWith("/")) {
                if (uri.startsWith(p)) return true;
            } else {
                if (uri.equals(p) || uri.startsWith(p)) return true;
            }
        }
        return false;
    }

    private void sendJsonUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401, \"message\":\"未登录或登录已过期\"}");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();
        System.out.println("Incoming request: " + uri);

        if (isWhitelisted(uri)) {
            return true;
        }

        Object loginType = request.getSession().getAttribute("logintype");

        if (loginType == null) {
            if (isAjax(request)) {
                sendJsonUnauthorized(response);
            } else {
                if (uri.startsWith("/app/")) {
                    response.sendRedirect("/loginapp.html");
                } else {
                    response.sendRedirect("/login.html");
                }
            }
            return false;
        }

        try {
            if (uri.startsWith("/app/")) {
                if (request.getSession().getAttribute("client") != null) {
                    return true;
                } else {
                    if (isAjax(request)) {
                        sendJsonUnauthorized(response);
                    } else {
                        response.sendRedirect("/loginapp.html");
                    }
                    return false;
                }
            } else {
                if (request.getSession().getAttribute("user") != null) {
                    return true;
                } else {
                    if (isAjax(request)) {
                        sendJsonUnauthorized(response);
                    } else {
                        response.sendRedirect("/login.html");
                    }
                    return false;
                }
            }
        } catch (Exception e) {
            if (isAjax(request)) {
                sendJsonUnauthorized(response);
            } else {
                response.sendRedirect("/login.html");
            }
            return false;
        }
    }
}
