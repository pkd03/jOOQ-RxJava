package vn.tnteco.demo.config;

import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình toàn cục cho RxJava 3.
 *
 * Xử lý UndeliverableException:
 * Trong lập trình Reactive, nếu một Single hoặc luồng gặp lỗi sau khi downstream đã bị hủy (cancel/dispose)
 * hoặc downstream đã nhận lỗi trước đó, RxJava sẽ đẩy lỗi đó vào RxJavaPlugins.onError.
 * Nếu không thiết lập errorHandler, lỗi này sẽ làm văng Thread.UncaughtExceptionHandler.
 * Thiết lập errorHandler ở đây giúp ghi log lại các lỗi này một cách an toàn mà không làm sập ứng dụng.
 */
@Slf4j
@Configuration
public class RxJavaConfig {

    @PostConstruct
    public void configureRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler(throwable -> {
            log.warn("RxJava UndeliverableException được chặn lại bởi global error handler: {}",
                    throwable.getMessage(), throwable);
        });
    }
}
