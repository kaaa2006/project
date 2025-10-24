package org.team.mealkitshop.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${uploadPath}")
    private String uploadDir; // 순수 경로 문자열 전제

    @PostConstruct
    void ensureUploadDir() throws Exception {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 순수 경로 → file:// URI로 변환하여 리소스 로케이션에 사용
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) location += "/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .resourceChain(true)
                .addResolver(new PathResourceResolver());


        // Windows 환경 안전하게 file URI 생성
        String cleanUploadDir = uploadDir.replace("\\", "/");
        if (cleanUploadDir.endsWith("/")) {
            cleanUploadDir = cleanUploadDir.substring(0, cleanUploadDir.length() - 1);
        }
        String uploadPathUri = "file:///" + cleanUploadDir + "/";

        // 공지사항, 이벤트 업로드 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPathUri)
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

    }
}
