package vn.tnteco.demo.data.repository;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import vn.tnteco.demo.data.dto.response.OrderResponse;
import vn.tnteco.demo.exception.AppException;
import vn.tnteco.demo.exception.ErrorCode;
import vn.tnteco.demo.jooq.tables.records.OrdersRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static vn.tnteco.demo.jooq.Tables.ORDERS;

/**
 * Repository thao tác bảng ORDERS và thực hiện transaction liên quan giữa ORDERS và PRODUCTS.
 */
@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final DSLContext dsl;
    private final ProductRepository productRepository;

    /**
     * Lấy toàn bộ danh sách đơn hàng của một user theo thứ tự mới nhất trước.
     */
    public Single<List<OrderResponse>> findByUserId(Long userId) {
        return Single.fromCallable(() -> dsl.selectFrom(ORDERS)
                .where(ORDERS.USER_ID.eq(userId))
                .orderBy(ORDERS.CREATED_AT.desc())
                .fetch()
                .map(this::mapToDto)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * Lấy N đơn hàng gần nhất của một user.
     */
    public Single<List<OrderResponse>> findRecentOrdersByUserId(Long userId, int limit) {
        return Single.fromCallable(() -> dsl.selectFrom(ORDERS)
                .where(ORDERS.USER_ID.eq(userId))
                .orderBy(ORDERS.CREATED_AT.desc())
                .limit(limit)
                .fetch()
                .map(this::mapToDto)
        ).subscribeOn(Schedulers.io());
    }

    /**
     * Tính tổng số tiền mà một user đã chi tiêu.
     * Sử dụng COALESCE để đảm bảo kết quả không bao giờ là null (luôn trả về BigDecimal.ZERO nếu chưa mua gì).
     */
    public Single<BigDecimal> calculateTotalSpentByUserId(Long userId) {
        return Single.fromCallable(() -> {
            BigDecimal total = dsl.select(DSL.coalesce(DSL.sum(ORDERS.AMOUNT), BigDecimal.ZERO))
                    .from(ORDERS)
                    .where(ORDERS.USER_ID.eq(userId))
                    .fetchOne(0, BigDecimal.class);

            return total != null ? total : BigDecimal.ZERO;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Tạo đơn hàng và trừ tồn kho sản phẩm trong CÙNG MỘT TRANSACTION của jOOQ.
     *
     * Cơ chế hoạt động:
     * 1. Bắt đầu transaction bằng dsl.transactionResult(...)
     * 2. Trừ kho: UPDATE products SET stock = stock - quantity WHERE id = ? AND stock >= quantity
     *    - Nếu số dòng cập nhật == 0 -> Kho không đủ hoặc race condition -> ném AppException(INSUFFICIENT_STOCK).
     *    - Việc ném RuntimeException trong transactionResult sẽ tự động ROLLBACK toàn bộ transaction.
     * 3. Chèn đơn hàng mới vào bảng ORDERS và RETURNING thông tin vừa chèn.
     * 4. Bọc toàn bộ block trong Single.fromCallable và chạy trên Schedulers.io().
     */
    public Single<OrderResponse> createOrderInTransaction(Long userId, Long productId, int quantity, BigDecimal unitPrice) {
        return Single.fromCallable(() -> dsl.transactionResult(configuration -> {
            DSLContext txDsl = DSL.using(configuration);

            // Bước 1: Trừ kho an toàn (atomic update)
            int updatedRows = productRepository.deductStock(txDsl, productId, quantity);
            if (updatedRows == 0) {
                // Ném exception để jOOQ tự động ROLLBACK transaction
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK,
                        "Số lượng tồn kho không đủ để đáp ứng đơn hàng cho sản phẩm id: " + productId);
            }

            // Bước 2: Tính tổng tiền đơn hàng
            BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

            // Bước 3: Chèn đơn hàng mới (dùng store() tương thích hoàn toàn cả H2 và PostgreSQL)
            OrdersRecord newOrder = txDsl.newRecord(ORDERS);
            newOrder.setUserId(userId);
            newOrder.setProductId(productId);
            newOrder.setQuantity(quantity);
            newOrder.setAmount(totalAmount);
            newOrder.setCreatedAt(LocalDateTime.now());
            newOrder.store();

            return mapToDto(newOrder);
        })).subscribeOn(Schedulers.io());
    }

    private OrderResponse mapToDto(OrdersRecord r) {
        return OrderResponse.builder()
                .id(r.getId())
                .userId(r.getUserId())
                .productId(r.getProductId())
                .quantity(r.getQuantity())
                .amount(r.getAmount())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
