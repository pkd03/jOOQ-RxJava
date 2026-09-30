package vn.tnteco.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Cấu hình Spring Web MVC, đăng ký SingleReturnValueHandler để hỗ trợ endpoint trả về Single<T>.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
        handlers.add(new SingleReturnValueHandler());
    }
}
