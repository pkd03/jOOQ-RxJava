package vn.tnteco.demo.data.repository;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import vn.tnteco.demo.data.dto.response.ProductResponse;
import vn.tnteco.demo.jooq.tables.records.ProductsRecord;

import java.util.Optional;

import static vn.tnteco.demo.jooq.Tables.PRODUCTS;

/**
 * Repository thao tác bảng PRODUCTS bằng jOOQ.
 * Mọi thao tác I/O blocking đều bọc trong Single.fromCallable và chạy trên Schedulers.io().
 */
@Repository
@RequiredArgsConstructor
public class ProductRepository {

    private final DSLContext dsl;

    /**
     * Tìm sản phẩm theo ID.
     * Trả về Single<Optional<ProductResponse>> - an toàn tuyệt đối với RxJava 3 (không bao giờ emit null).
     */
    public Single<Optional<ProductResponse>> findById(Long id) {
        return Single.fromCallable(() -> {
            Optional<ProductsRecord> recordOpt = dsl.selectFrom(PRODUCTS)
                    .where(PRODUCTS.ID.eq(id))
                    .fetchOptional();

            return recordOpt.map(this::mapToDto);
        }).subscribeOn(Schedulers.io());
    }

    /**
     * Giảm số lượng tồn kho của sản phẩm có kiểm tra tồn kho đủ lớn hơn hoặc bằng quantity.
     * Thao tác atomic trong database để ngăn chặn Race Condition.
     *
     * @param txDsl context trong transaction
     * @return số bản ghi được cập nhật (1: thành công, 0: không đủ hàng hoặc sản phẩm không tồn tại)
     */
    public int deductStock(DSLContext txDsl, Long productId, int quantity) {
        return txDsl.update(PRODUCTS)
                .set(PRODUCTS.STOCK, PRODUCTS.STOCK.minus(quantity))
                .where(PRODUCTS.ID.eq(productId).and(PRODUCTS.STOCK.ge(quantity)))
                .execute();
    }

    private ProductResponse mapToDto(ProductsRecord r) {
        return ProductResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .price(r.getPrice())
                .stock(r.getStock())
                .build();
    }
}
