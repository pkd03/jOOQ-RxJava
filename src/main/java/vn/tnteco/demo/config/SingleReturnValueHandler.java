package vn.tnteco.demo.config;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Adapter cho phép Spring Web MVC xử lý kiểu trả về Single<T> của RxJava 3.
 *
 * Cơ chế hoạt động:
 * 1. Khi một Controller trả về Single<T>, Handler này chặn lại và chuyển đổi thành DeferredResult<Object>.
 * 2. Đăng ký subscriber với Single:
 *    - Khi Single phát ra dữ liệu (onSuccess) -> deferredResult.setResult(result).
 *    - Khi Single phát ra lỗi (onError) -> deferredResult.setErrorResult(error).
 * 3. Khi setErrorResult được gọi, Spring MVC sẽ điều hướng luồng xử lý vào các method @ExceptionHandler
 *    của GlobalExceptionHandler một cách liền mạch, giữ trọn vẹn context HTTP.
 * 4. Nếu request bị timeout hoặc hoàn tất, tự động dispose Disposable của RxJava để tránh leak tài nguyên.
 */
public class SingleReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return Single.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
        return returnValue instanceof Single;
    }

    @Override
    public void handleReturnValue(Object returnValue,
                                  MethodParameter returnType,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest) throws Exception {
        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }

        final DeferredResult<Object> deferredResult = new DeferredResult<>();
        WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult, mavContainer);

        Single<?> single = (Single<?>) returnValue;
        Disposable disposable = single.subscribe(
                deferredResult::setResult,
                deferredResult::setErrorResult
        );

        deferredResult.onCompletion(disposable::dispose);
        deferredResult.onTimeout(disposable::dispose);
    }
}
