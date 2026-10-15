package com.explore.platform.security;

import com.explore.platform.common.result.ErrorCode;
import com.explore.platform.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证（未登录 / Token 失效）入口点：返回统一 JSON 与 401 状态码
 * <p>
 * 默认情况下无认证机制时 Spring Security 返回 403，前端无法区分"未登录"，
 * 因此自定义入口点明确返回 401。
 *
 * @author explore-platform
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    // commence：处理未认证请求，返回统一 JSON 与 401 状态码
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        //AuthenticationException：认证异常，包含未认证的原因，如"未登录"、"Token 过期"等
        //response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)：设置响应状态码为 401，未认证访问
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //response.setContentType(MediaType.APPLICATION_JSON_VALUE)：设置响应内容类型为 JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        //response.setCharacterEncoding(StandardCharsets.UTF_8.name())：设置响应字符编码为 UTF-8
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        //objectMapper.writeValueAsString(body)：将 Result 对象转换为 JSON 字符串
        //response.getWriter().write(objectMapper.writeValueAsString(body))：将 JSON 字符串写入响应体
        Result<Void> body = Result.fail(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
//    触发条件：
//请求进入权限校验阶段，当前 SecurityContext 没有有效的 Authentication（没有登录凭证），并且接口要求认证（.authenticated()）
//常见场景：
//请求头没有携带 Authorization: Bearer xxx
//token 过期、签名错误，JWT 过滤器解析失败，SecurityContext 为空
//token 篡改，解析抛异常
//✅ 只会在未登录 / 没有认证信息的时候触发，返回 401
//❗ 已经登录但是权限不足（角色不够）不会走这里！ 会交给 AccessDeniedHandler 返回 403。
}
