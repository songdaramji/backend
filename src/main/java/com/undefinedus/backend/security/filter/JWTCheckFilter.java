package com.undefinedus.backend.security.filter;

import com.google.gson.Gson;
import com.undefinedus.backend.dto.MemberSecurityDTO;
import com.undefinedus.backend.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JWTCheckFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        String path = request.getRequestURI();

        // TODO : 나중에 실제 배포할 때에는 JWT토큰 입력 받아야 하는 건 빼기
        return path.contains("/api/member/login") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars/") ||
                path.contains("/api/member/register") ||
                path.contains("/api/member/refresh") ||
                path.contains("/api/member/email/send-verification") ||  // 이메일 인증 발송
                path.contains("/api/member/email/verify") || // 이메일 인증 확인
                path.contains("/api/book/**") || // 나중에 제거 필요
                path.contains("/api/discussion/**") || // 나중에 제거 필요
                path.contains("/api/discussionComment/**"); // 나중에 제거 필요
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeaderStr = request.getHeader("Authorization");

        // JWT 토큰이 없는 경우 바로 다음 필터로 진행
        if(authHeaderStr == null || !authHeaderStr.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = authHeaderStr.substring(7);
            Map<String, Object> claims = JWTUtil.validateToken(accessToken);
            
            String username = (String) claims.get("username");
            Long id = ((Number) claims.get("id")).longValue();
            String nickname = (String) claims.get("nickname");
            List<String> roles = (List<String>) claims.get("roles");
            String socialProvider = (String) claims.get("socialProvider");
            
            
            // 이미 로그인으로 인증된 사용자의 토큰이므로
            // password는 빈 문자열로 설정해도 됨
            MemberSecurityDTO memberSecurityDTO = new MemberSecurityDTO(
                    username,
                    "", // 빈 문자열 또는 임의의 값으로 설정 (실제로 사용되지 않음)
                    id,
                    nickname,
                    roles,
                    socialProvider
            );
            
            // 인증 객체 생성 시 credentials(password)도 null로 설정
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            memberSecurityDTO,
                            null, // 이미 토큰으로 인증되었으므로 credentials 불필요
                            memberSecurityDTO.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);

        } catch (Exception e) {

            Gson gson = new Gson();

            String msg = gson.toJson(Map.of(
                    "error", "ERROR_ACCESS_TOKEN",
                    "message", e.getMessage(),
                    "cause", e.getCause() != null ? e.getCause().toString() : "No cause available"
            ));

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter printWriter = response.getWriter();
            printWriter.println(msg);
            printWriter.close();
        }

    }
}
