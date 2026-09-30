# Dự Án Mẫu Thực Hành jOOQ + RxJava 3 (Spring Boot 3)

Dự án mẫu thực hành kết hợp **jOOQ** (truy vấn SQL an toàn kiểu dữ liệu) và **RxJava 3** (lập trình phản ứng Reactive), tuân thủ chuẩn quy ước cấu trúc package của hệ thống doanh nghiệp, xử lý exception tập trung theo mã lỗi chuẩn (`ErrorCode`) và kiểm thử bằng `TestObserver`.

---

## 1. Công nghệ sử dụng
- **Java 17**, **Maven**
- **Spring Boot 3.3.4** (Spring Web MVC với custom `SingleReturnValueHandler` adapter chuyển `Single<T>` -> `DeferredResult<T>`)
- **RxJava 3**: `Single`, `Schedulers.io()`, `TestObserver`
- **jOOQ 3.19.x** (tự động sinh mã nguồn Records, Tables từ schema bằng plugin `jooq-codegen-maven`)
- **H2 in-memory Database** (chế độ PostgreSQL compatibility: `MODE=PostgreSQL`)
- **Apache Commons Lang 3** (`Pair`)
- **JUnit 5**, **Mockito**, **Spring Boot Test**

---

## 2. Sơ đồ cấu trúc Package (`vn.tnteco.demo`)

Dự án tuân thủ nghiêm ngặt quy ước phân chia tầng và đặt tên theo chuẩn dự án thực tế:

```
src/main/
├── resources/
│   ├── schema.sql                         # DDL tạo bảng: users, products, orders
│   ├── data.sql                           # Dữ liệu mẫu (5 users, 5 products, 10 orders)
│   └── application.properties             # Cấu hình H2, jOOQ dialect H2/PostgreSQL
└── java/
    └── vn.tnteco.demo/
        ├── DemoApplication.java           # Main class Spring Boot 3
        │
        ├── config/                        # Cấu hình hệ thống & Adapter Spring MVC
        │   ├── SingleReturnValueHandler.java # AsyncHandler adapter Single<T> -> DeferredResult<T>
        │   ├── WebMvcConfig.java             # Đăng ký adapter vào WebMvcConfigurer
        │   └── RxJavaConfig.java             # Quản lý lỗi UndeliverableException toàn cục
        │
        ├── exception/                     # Chuẩn hóa mã lỗi & Exception tập trung
        │   ├── ErrorCode.java                # Enum mã lỗi (USER_NOT_FOUND, INSUFFICIENT_STOCK, ...)
        │   ├── AppException.java             # RuntimeException bọc ErrorCode
        │   ├── ErrorResponse.java            # DTO chuẩn trả về client: { code, message, timestamp, path }
        │   └── GlobalExceptionHandler.java   # @RestControllerAdvice (unwrap CompositeException, map jOOQ)
        │
        ├── validate/                      # Tầng kiểm tra tính hợp lệ dữ liệu
        │   └── OrderRequestValidator.java    # Kiểm tra quantity > 0, ném AppException(INVALID_REQUEST)
        │
        ├── data/                          # Tầng dữ liệu & Repository dùng jOOQ
        │   ├── dto/
        │   │   ├── CreateOrderRequest.java       # Request tạo đơn hàng (record)
        │   │   ├── UserRecordDto.java            # DTO người dùng (record)
        │   │   ├── ProductRecordDto.java         # DTO sản phẩm (record)
        │   │   ├── OrderRecordDto.java           # DTO đơn hàng (record)
        │   │   ├── UserProfileResponse.java      # Response thông tin profile (record)
        │   │   ├── UserDashboardResponse.java    # Response thông tin dashboard (record)
        │   │   └── ProductAvailabilityResponse.java
        │   └── repository/                # Trả về Single<Optional<T>> hoặc Single<List<T>>, bọc jOOQ blocking
        │       ├── UserRepository.java           # Truy vấn users
        │       ├── ProductRepository.java        # Truy vấn products, trừ kho atomic
        │       └── OrderRepository.java          # Tạo order + trừ kho trong jOOQ Transaction
        │
        ├── service/                       # Tầng nghiệp vụ: flatMap, zip, Pair, retry, fallback
        │   ├── UserService.java              # Logic User: get (flatMap), profile (flatMap + Pair), dashboard (Single.zip)
        │   ├── ProductService.java          # Logic Product: retry lỗi tạm thời & onErrorResumeNext fallback
        │   └── OrderService.java            # Logic Order: chuỗi flatMap + transaction (không retry thao tác ghi)
        │
        └── wio.rest/                      # Giao tiếp Web I/O
            ├── UserOperations.java           # Interface định nghĩa API User (annotation @GetMapping, Single<...>)
            ├── OrderOperations.java          # Interface định nghĩa API Order (annotation @PostMapping, Single<...>)
            ├── ProductOperations.java        # Interface định nghĩa API Product (annotation @GetMapping, Single<...>)
            ├── consumer/
            │   └── OrderEventConsumer.java   # Consumer mẫu theo đúng quy ước cấu trúc dự án
            └── controller/
                ├── UserController.java       # Implement UserOperations (chỉ delegate sang UserService)
                ├── OrderController.java      # Implement OrderOperations (gọi validator + OrderService)
                └── ProductController.java    # Implement ProductOperations (chỉ delegate sang ProductService)

target/generated-sources/jooq/             # Code Tables, Records sinh tự động bởi jOOQ (tách biệt khỏi src/main)
```

---

## 3. Cách biên dịch và chạy dự án

### Yêu cầu môi trường
- JDK 17+
- Maven 3.8+ (hoặc dùng wrapper `./mvnw` / `mvnw.cmd` có sẵn trong project)

### Sinh mã jOOQ và Biên dịch
Plugin `jooq-codegen-maven` được cấu hình tự động đọc schema từ `src/main/resources/schema.sql` và sinh code:
```bash
./mvnw clean compile
# Hoặc trên Windows:
mvnw.cmd clean compile
```

### Chạy Unit & Integration Test
Toàn bộ 20 bài test (sử dụng `TestObserver` của RxJava 3 và `MockMvc`) kiểm thử:
- flatMap thành công & flatMap gặp `USER_NOT_FOUND`
- `Single.zip` song song 3 nguồn thành công & ngắt stream khi 1 nguồn lỗi
- Tạo order khi hết hàng (`INSUFFICIENT_STOCK`) và xác nhận **Transaction Rollback** (kho không bị trừ)
- Retry lỗi tạm thời 2 lần và thành công ở lần 3
- Fallback `onErrorResumeNext` khi vượt quá số lần retry
- Validator ném `INVALID_REQUEST` khi `quantity <= 0`
- Toàn bộ các REST Controller endpoint

Chạy lệnh test:
```bash
./mvnw test
# Hoặc trên Windows:
mvnw.cmd test
```

### Khởi chạy ứng dụng
```bash
./mvnw spring-boot:run
# Hoặc trên Windows:
mvnw.cmd spring-boot:run
```
Ứng dụng sẽ khởi chạy tại cổng `http://localhost:8080`.
H2 Console truy cập tại: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:demo`, User: `sa`, Password để trống).

---

## 4. Hướng dẫn Test các API bằng `curl`

### API 1: `GET /api/users/{id}` (Lấy thông tin User)
- **Trường hợp thành công (User tồn tại):**
  ```bash
  curl -X GET http://localhost:8080/api/users/1
  ```
  *Response (200 OK):*
  ```json
  {
    "id": 1,
    "name": "Nguyen Van A",
    "email": "nguyenvana@tnteco.vn",
    "age": 28,
    "status": "ACTIVE"
  }
  ```

- **Trường hợp lỗi (User không tồn tại -> 404 USER_NOT_FOUND):**
  ```bash
  curl -X GET http://localhost:8080/api/users/99
  ```
  *Response (404 Not Found):*
  ```json
  {
    "code": "USER_NOT_FOUND",
    "message": "Không tìm thấy người dùng với id: 99",
    "timestamp": "2026-09-29T11:30:00",
    "path": "/api/users/99"
  }
  ```

---

### API 2: `GET /api/users/{id}/profile` (flatMap & Pair)
Lấy thông tin user trước, sau đó lấy danh sách đơn hàng của user đó. Dùng `Pair.of(user, orders)` để chuyển tiếp dữ liệu qua các bước.
```bash
curl -X GET http://localhost:8080/api/users/1/profile
```
*Response (200 OK):*
```json
{
  "user": {
    "id": 1,
    "name": "Nguyen Van A",
    "email": "nguyenvana@tnteco.vn",
    "age": 28,
    "status": "ACTIVE"
  },
  "orders": [
    {
      "id": 8,
      "userId": 1,
      "productId": 5,
      "quantity": 1,
      "amount": 4200000.00,
      "createdAt": "2026-09-22T08:50:00"
    },
    {
      "id": 2,
      "userId": 1,
      "productId": 2,
      "quantity": 1,
      "amount": 2300000.00,
      "createdAt": "2026-09-05T14:30:00"
    },
    {
      "id": 1,
      "userId": 1,
      "productId": 1,
      "quantity": 1,
      "amount": 1750000.00,
      "createdAt": "2026-09-01T10:15:00"
    }
  ]
}
```

---

### API 3: `GET /api/users/{id}/dashboard` (Single.zip chạy song song)
Chạy song song 3 truy vấn độc lập trên các thread `Schedulers.io()` riêng biệt:
1. Thông tin user
2. 5 đơn hàng gần nhất
3. Tổng số tiền đã chi tiêu
```bash
curl -X GET http://localhost:8080/api/users/1/dashboard
```
*Response (200 OK):*
```json
{
  "user": {
    "id": 1,
    "name": "Nguyen Van A",
    "email": "nguyenvana@tnteco.vn",
    "age": 28,
    "status": "ACTIVE"
  },
  "recentOrders": [
    {
      "id": 8,
      "userId": 1,
      "productId": 5,
      "quantity": 1,
      "amount": 4200000.00,
      "createdAt": "2026-09-22T08:50:00"
    }
  ],
  "totalSpent": 8250000.00,
  "executionDurationMs": 15
}
```
*(Trên console server sẽ xuất hiện log rõ ràng của 3 thread khác nhau hoàn tất và thời gian thực thi song song).*

---

### API 4: `POST /api/orders` (Tạo đơn hàng & Transaction jOOQ)
Thực hiện chuỗi tuần tự `flatMap`: validate dữ liệu -> kiểm tra user tồn tại & `ACTIVE` -> kiểm tra product tồn tại & đủ tồn kho -> trừ kho và tạo order trong cùng 1 transaction jOOQ.

- **Trường hợp thành công:**
  ```bash
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{ "userId": 1, "productId": 1, "quantity": 2 }'
  ```
  *Response (200 OK):*
  ```json
  {
    "id": 11,
    "userId": 1,
    "productId": 1,
    "quantity": 2,
    "amount": 3500000.00,
    "createdAt": "2026-09-29T11:32:00"
  }
  ```

- **Trường hợp lỗi Validate (quantity <= 0 -> 400 INVALID_REQUEST):**
  ```bash
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{ "userId": 1, "productId": 1, "quantity": 0 }'
  ```
  *Response (400 Bad Request):*
  ```json
  {
    "code": "INVALID_REQUEST",
    "message": "Số lượng đặt hàng (quantity) phải lớn hơn 0",
    "timestamp": "2026-09-29T11:32:10",
    "path": "/api/orders"
  }
  ```

- **Trường hợp User bị khóa (status = INACTIVE -> 422 USER_INACTIVE):**
  *(User id = 3 là Le Van C có status INACTIVE trong data.sql)*
  ```bash
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{ "userId": 3, "productId": 1, "quantity": 1 }'
  ```
  *Response (422 Unprocessable Entity):*
  ```json
  {
    "code": "USER_INACTIVE",
    "message": "Tài khoản user id=3 đang bị khóa hoặc không hoạt động",
    "timestamp": "2026-09-29T11:32:20",
    "path": "/api/orders"
  }
  ```

- **Trường hợp Kho không đủ hàng (-> 409 INSUFFICIENT_STOCK & Rollback):**
  *(Product id = 4 có tồn kho = 0 trong data.sql)*
  ```bash
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d '{ "userId": 1, "productId": 4, "quantity": 1 }'
  ```
  *Response (409 Conflict):*
  ```json
  {
    "code": "INSUFFICIENT_STOCK",
    "message": "Kho chỉ còn 0 sản phẩm, không đủ số lượng yêu cầu: 1",
    "timestamp": "2026-09-29T11:32:30",
    "path": "/api/orders"
  }
  ```

---

### API 5: `GET /api/products/{id}/availability` (Retry & Fallback)
- **Trường hợp bình thường:**
  ```bash
  curl -X GET http://localhost:8080/api/products/1/availability
  ```
  *Response (200 OK):*
  ```json
  {
    "productId": 1,
    "productName": "Ban phim co Keychron K2",
    "stock": 15,
    "available": true,
    "note": "Còn hàng (15 sản phẩm)"
  }
  ```

- **Sản phẩm hết hàng:**
  ```bash
  curl -X GET http://localhost:8080/api/products/4/availability
  ```
  *Response (200 OK):*
  ```json
  {
    "productId": 4,
    "productName": "Tai nghe Sony WH-1000XM5",
    "stock": 0,
    "available": false,
    "note": "Đã hết hàng"
  }
  ```

---

## 5. Giải thích chuyên sâu: `flatMap` vs `zip` vs `Pair` trong RxJava 3

Dưới đây là phần so sánh trực quan và dễ nhớ nhất dựa trên chính code thực tế trong dự án:

### 1. Khi nào dùng `flatMap`? (Quan hệ Phụ thuộc Tuần tự - Sequential)
- **Bản chất**: `flatMap` biến đổi phần tử phát ra từ luồng này thành **một luồng Single mới khác**, sau đó "làm phẳng" (flatten) để luồng tiếp tục chảy, tránh bị lồng nhau kiểu `Single<Single<T>>`.
- **Ngữ cảnh sử dụng**: Khi **bước sau bắt buộc phải có kết quả của bước trước** mới thực hiện được.
- **Ví dụ trong code (`UserService.java`):**
  ```java
  // Bước 1: Lấy user trước
  getUserById(id)
      // Bước 2: Chỉ khi có user, ta mới lấy orders của user đó (user.id())
      .flatMap(user -> orderRepository.findByUserId(user.id()))
  ```
  Nếu User không tồn tại, luồng sẽ rẽ sang `Single.error(...)` và dừng ngay lập tức, không bao giờ gọi tới truy vấn lấy đơn hàng ở bước 2.

---

### 2. Khi nào dùng `Single.zip`? (Quan hệ Độc lập Song song - Parallel)
- **Bản chất**: `zip` nhận vào nhiều Single độc lập, thực thi chúng đồng thời (khi kết hợp `subscribeOn(Schedulers.io())` cho từng Single) và gom tất cả kết quả của các nguồn lại thành một kết quả chung duy nhất thông qua một hàm gộp (Zipper function).
- **Ngữ cảnh sử dụng**: Khi các luồng dữ liệu **hoàn toàn độc lập với nhau**, không cái nào phụ thuộc vào kết quả của cái nào.
- **Ví dụ trong code (`UserService.java` - API Dashboard):**
  ```java
  Single<UserRecordDto> singleUser = getUserById(id).subscribeOn(Schedulers.io());
  Single<List<OrderRecordDto>> singleRecentOrders = orderRepository.findRecentOrdersByUserId(id, 5).subscribeOn(Schedulers.io());
  Single<BigDecimal> singleTotalSpent = orderRepository.calculateTotalSpentByUserId(id).subscribeOn(Schedulers.io());

  // Cả 3 nguồn chạy trên 3 worker thread khác nhau
  return Single.zip(singleUser, singleRecentOrders, singleTotalSpent, 
          (user, recentOrders, totalSpent) -> new UserDashboardResponse(user, recentOrders, totalSpent, duration));
  ```
  - **Hiệu năng:** Nếu dùng `flatMap`, thời gian thực thi là $T = T_1 + T_2 + T_3$. Khi dùng `Single.zip`, thời gian đáp ứng xấp xỉ $\max(T_1, T_2, T_3)$.
  - **Xử lý lỗi:** Nếu bất kỳ 1 nguồn nào lỗi, `Single.zip` sẽ ngắt stream và đẩy lỗi đó xuống downstream ngay lập tức.

---

### 3. Khi nào dùng `Pair` (Apache Commons Lang)?
- **Vấn đề trong Reactive Streams**: Khi chuyển qua các toán tử `flatMap`, phạm vi (scope) của biến ở bước trước sẽ không còn truy cập được ở các bước tiếp theo, trừ khi ta tạo thêm các class DTO phụ tạm thời.
- **Giải pháp**: `Pair.of(left, right)` đóng vai trò là một "chiếc túi đựng 2 ngăn" để mang theo (carry) đồng thời cả dữ liệu bước trước và dữ liệu bước hiện tại sang bước kế tiếp.
- **Ví dụ trong code (`UserService.java` - API Profile):**
  ```java
  getUserById(id)
      .flatMap(user -> orderRepository.findByUserId(user.id())
              // Dùng Pair để gói cả 'user' (bước 1) và 'orders' (bước 2)
              .map(orders -> Pair.of(user, orders)))
      // Ở đây ta có cả 2 đối tượng để tạo UserProfileResponse mà không cần tạo class trung gian
      .map(pair -> new UserProfileResponse(pair.getLeft(), pair.getRight()));
  ```

---

### 4. Tại sao KHÔNG ĐƯỢC RETRY trên thao tác GHI (Insert Order)?
- **Tính Idempotent**: Thao tác đọc (GET) có tính **Idempotent** (gọi 1 lần hay nhiều lần đều không thay đổi trạng thái hệ thống). Do đó, việc tự động retry 1-2 lần khi mất kết nối tạm thời (`SQLTransientException`) ở `ProductService` là an toàn.
- **Tính Non-Idempotent**: Thao tác tạo đơn hàng và trừ kho là **Non-Idempotent** (thay đổi trạng thái dữ liệu). Giả sử mạng bị timeout sau khi database đã trừ kho và insert đơn thành công nhưng gói tin ACK chưa kịp phản hồi:
  1. Nếu tự động retry, hệ thống sẽ thực hiện lần ghi thứ 2.
  2. Khách hàng bị trừ kho 2 lần và tạo 2 đơn hàng trùng lặp.
- **Quy tắc vàng**: Với thao tác ghi, khi gặp lỗi phải ném ngoại lệ rõ ràng cho client/hệ sinh thái bên ngoài xử lý, tuyệt đối không dùng retry tự động!
