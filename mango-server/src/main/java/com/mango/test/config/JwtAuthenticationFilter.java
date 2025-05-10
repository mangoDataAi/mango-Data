package com.mango.test.config;

import com.mango.test.util.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TOKEN_PARAM = "TOKEN";
    private static final String TOKEN_COOKIE = "TOKEN";

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.isNotBlank(token)) {
            try {
                String username = jwtUtils.getUsernameFromToken(token);
                if (StringUtils.isNotBlank(username)) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.error("Token验证失败", e);
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String token = null;

        // 1. 从请求头中获取
        String authHeader = request.getHeader(TOKEN_HEADER);
        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith(TOKEN_PREFIX)
            && !"undefined".equalsIgnoreCase(authHeader) && !"null".equalsIgnoreCase(authHeader)) {
            token = authHeader.substring(TOKEN_PREFIX.length());
            if ("undefined".equalsIgnoreCase(token) || "null".equalsIgnoreCase(token)) {
                token = null;
            }
        }

        // 2. 从请求参数中获取
        if (StringUtils.isBlank(token)) {
            token = request.getParameter(TOKEN_PARAM);
            if (StringUtils.isBlank(token) || "undefined".equalsIgnoreCase(token)
                || "null".equalsIgnoreCase(token)) {
                token = null;
            }
        }

        // 3. 从Cookie中获取
        if (StringUtils.isBlank(token) && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (TOKEN_COOKIE.equals(cookie.getName())) {
                    token = cookie.getValue();
                    if (StringUtils.isBlank(token) || "undefined".equalsIgnoreCase(token)
                        || "null".equalsIgnoreCase(token)) {
                        token = null;
                    }
                    break;
                }
            }
        }

        // 4. 从请求头的自定义字段中获取
        if (StringUtils.isBlank(token)) {
            token = request.getHeader(TOKEN_PARAM);
            if (StringUtils.isBlank(token) || "undefined".equalsIgnoreCase(token)
                || "null".equalsIgnoreCase(token)) {
                token = null;
            }
        }

        // 5. 验证token的基本格式
        if (StringUtils.isNotBlank(token)) {
            token = token.trim();
            // 验证token长度
            if (token.length() < 10) { // JWT token通常会很长
                log.warn("Invalid token length: {}", token);
                return null;
            }
            // 验证token是否包含JWT的三个部分（用.分隔）
            if (!token.contains(".") || token.split("\\.").length != 3) {
                log.warn("Invalid token format: {}", token);
                return null;
            }
        }

        return token;
    }
}
