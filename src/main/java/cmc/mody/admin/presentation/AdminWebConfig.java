package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminSessionService;
import cmc.mody.common.admin.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AdminWebConfig implements WebMvcConfigurer {
    private final ObjectProvider<AdminSessionService> sessions;
    private final ObjectProvider<AdminAccessService> access;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        sessions.ifAvailable(service -> registry
            .addInterceptor(new AdminRequestInterceptor(service, access.getObject()))
            .addPathPatterns("/api/v1/admin/**")
            .excludePathPatterns("/api/v1/admin/auth/**"));
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("Referrer-Policy", "no-referrer");
                return true;
            }
        }).addPathPatterns("/admin/**", "/api/v1/admin/**", "/admin").order(-1);
    }
}
