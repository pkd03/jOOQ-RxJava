-- Dữ liệu mẫu bảng users (5 users)
-- User 1, 2, 4, 5: ACTIVE
-- User 3: INACTIVE (để test case tài khoản không hoạt động)
INSERT INTO users (id, name, email, age, status) VALUES
(1, 'Nguyen Van A', 'nguyenvana@tnteco.vn', 28, 'ACTIVE'),
(2, 'Tran Thi B', 'tranthib@tnteco.vn', 24, 'ACTIVE'),
(3, 'Le Van C', 'levanc@tnteco.vn', 35, 'INACTIVE'),
(4, 'Pham Thi D', 'phamthid@tnteco.vn', 22, 'ACTIVE'),
(5, 'Hoang Van E', 'hoangvane@tnteco.vn', 40, 'ACTIVE');

-- Dữ liệu mẫu bảng products (5 products)
-- Product 4 có stock = 0 để test case hết hàng
INSERT INTO products (id, name, price, stock) VALUES
(1, 'Ban phim co Keychron K2', 1750000.00, 15),
(2, 'Chuot khong day Logitech MX Master 3S', 2300000.00, 8),
(3, 'Man hinh Dell UltraSharp U2723QE', 11500000.00, 3),
(4, 'Tai nghe Sony WH-1000XM5', 6890000.00, 0),
(5, 'Ghe cong thai hoc Ergonomic', 4200000.00, 10);

-- Dữ liệu mẫu bảng orders (10 orders)
INSERT INTO orders (id, user_id, product_id, quantity, amount, created_at) VALUES
(1, 1, 1, 1, 1750000.00, '2026-09-01 10:15:00'),
(2, 1, 2, 1, 2300000.00, '2026-09-05 14:30:00'),
(3, 2, 1, 2, 3500000.00, '2026-09-10 09:00:00'),
(4, 2, 5, 1, 4200000.00, '2026-09-12 16:45:00'),
(5, 4, 2, 1, 2300000.00, '2026-09-15 11:20:00'),
(6, 4, 3, 1, 11500000.00, '2026-09-18 13:00:00'),
(7, 5, 5, 2, 8400000.00, '2026-09-20 15:10:00'),
(8, 1, 5, 1, 4200000.00, '2026-09-22 08:50:00'),
(9, 2, 2, 1, 2300000.00, '2026-09-25 17:05:00'),
(10, 5, 1, 1, 1750000.00, '2026-09-27 19:30:00');

-- Cập nhật sequence generator cho id nếu H2 tự sinh
ALTER TABLE users ALTER COLUMN id RESTART WITH 6;
ALTER TABLE products ALTER COLUMN id RESTART WITH 6;
ALTER TABLE orders ALTER COLUMN id RESTART WITH 11;
