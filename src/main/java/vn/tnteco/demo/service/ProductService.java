package vn.tnteco.demo.service;

import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tnteco.demo.data.dto.response.PageResponse;
import vn.tnteco.demo.data.dto.response.ProductAvailabilityResponse;
import vn.tnteco.demo.data.dto.response.ProductResponse;
import vn.tnteco.demo.data.repository.ProductRepository;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;

import java.io.IOException;
import java.sql.SQLTransientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Tìm sản phẩm theo id.
     * Dùng Single.defer(...) để mỗi lần subscribe / retry thì hàm productRepository.findById(id)
     * sẽ được thực thi lại mới hoàn toàn, thay vì dùng lại instance Single cũ đã emit lỗi.
     * Dùng flatMap để chuyển Optional sang ProductResponse hoặc ném PRODUCT_NOT_FOUND.
     */
    public Single<ProductResponse> getProductById(Long id) {
        return Single.defer(() -> productRepository.findById(id))
                .flatMap(productOpt -> productOpt
                        .map(Single::just)
                        .orElseGet(() -> Single.error(new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                                "Không tìm thấy sản phẩm với id: " + id))));
    }

    public Single<PageResponse<ProductResponse>> getAllProduct(int page, int size){
        return Single.zip(
                productRepository.getAllProduct(page, size),
                productRepository.countProducts(),
                (items, totalElements) -> {
                    int totalPages = (int) Math.ceil((double) totalElements / size);
                    boolean hasNext = page + 1 < totalPages;
                    return PageResponse.<ProductResponse>builder()
                            .items(items)
                            .page(page)
                            .size(size)
                            .totalElements(totalElements)
                            .totalPages(totalPages)
                            .hasNext(hasNext)
                            .build();
            }
        );
    }

    /**
     * API 5: Kiểm tra tính sẵn sàng / tồn kho của sản phẩm.
     *
     * GIẢI THÍCH TẠI SAO ĐƯỢC PHÉP RETRY Ở THAO TÁC ĐỌC:
     * - Thao tác đọc (Read-only / Idempotent): Không làm biến đổi trạng thái của database dù có gọi 1 lần hay 100 lần.
     * - Khi gặp các lỗi tạm thời (Transient Errors: ngắt kết nối mạng chốc lát, timeout nhẹ, lock wait...),
     *   việc retry tự động 2-3 lần giúp tăng tỷ lệ thành công (availability) mà không gây rủi ro dữ liệu.
     * - Chúng ta chỉ retry các lỗi tạm thời (isTransientError), KHÔNG retry các lỗi nghiệp vụ như PRODUCT_NOT_FOUND.
     *
     * GIẢI THÍCH onErrorResumeNext:
     * - Trong trường hợp đã retry tối đa 2 lần mà hệ thống DB/Network vẫn lỗi, thay vì để sập cả trang hoặc
     *   bắn 500 ra giao diện người dùng, toán tử onErrorResumeNext cho phép "rẽ nhánh" sang một luồng dự phòng (Fallback).
     * - Trả về trạng thái dự phòng an toàn (ví dụ: thông báo tạm thời không kiểm tra được tồn kho).
     */
    public Single<ProductAvailabilityResponse> checkAvailability(Long id) {
        return getProductById(id)
                // Retry tối đa 2 lần (tổng cộng 3 lần gọi) và chỉ retry khi gặp lỗi tạm thời
                .retry((attempt, throwable) -> {
                    boolean shouldRetry = attempt <= 2 && isTransientError(throwable);
                    if (shouldRetry) {
                        log.warn("[RETRY] Thử lại lần {} do gặp lỗi tạm thời khi đọc productId={}: {}",
                                attempt, id, throwable.getMessage());
                    }
                    return shouldRetry;
                })
                .map(product -> {
                    boolean isAvailable = product.getStock() != null && product.getStock() > 0;
                    String note = isAvailable
                            ? "Còn hàng (" + product.getStock() + " sản phẩm)"
                            : "Đã hết hàng";
                    return ProductAvailabilityResponse.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .stock(product.getStock())
                            .available(isAvailable)
                            .note(note)
                            .build();
                })
                // Fallback khi lỗi cơ sở hạ tầng kéo dài sau khi đã retry
                .onErrorResumeNext(throwable -> {
                    // Nếu là lỗi nghiệp vụ (ví dụ PRODUCT_NOT_FOUND), ta giữ nguyên để ném ra cho client biết
                    if (throwable instanceof AppException appEx && appEx.getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND) {
                        return Single.error(throwable);
                    }

                    // Nếu là lỗi tạm thời hoặc lỗi hạ tầng sau khi đã thử lại bất thành, fallback an toàn
                    log.error("[FALLBACK] Kích hoạt fallback cho productId={} sau khi retry thất bại: {}",
                            id, throwable.getMessage());
                    return Single.just(ProductAvailabilityResponse.builder()
                            .productId(id)
                            .productName("Sản phẩm tạm thời không khả dụng")
                            .stock(0)
                            .available(false)
                            .note("Hệ thống kiểm tra kho đang bảo trì, vui lòng kiểm tra lại sau.")
                            .build());
                });
    }

    /**
     * Kiểm tra xem ngoại lệ có phải là lỗi tạm thời (có thể hồi phục bằng cách thử lại) hay không.
     */
    private boolean isTransientError(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        return throwable instanceof SQLTransientException
                || throwable instanceof IOException
                || (throwable.getMessage() != null && throwable.getMessage().toLowerCase().contains("transient"));
    }
}
