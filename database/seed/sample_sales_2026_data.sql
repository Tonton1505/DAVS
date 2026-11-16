-- ============================================================================
-- Dữ liệu mẫu phục vụ kiểm thử phân tích doanh số 2026 trên DAVS
-- Bao gồm các trường: Ma_KH, Ten_KH, Vung, Ngay_DH, Doanh_so, Loi_nhuan, So_luong
-- ============================================================================

CREATE TABLE IF NOT EXISTS sample_sales_2026 (
    id         SERIAL PRIMARY KEY,
    ma_kh      VARCHAR(32)    NOT NULL,
    ten_kh     VARCHAR(150)   NOT NULL,
    vung       VARCHAR(64)    NOT NULL,
    ngay_dh    DATE           NOT NULL,
    doanh_so   NUMERIC(15, 2) NOT NULL,
    loi_nhuan  NUMERIC(15, 2) NOT NULL,
    so_luong   INT            NOT NULL
);

INSERT INTO sample_sales_2026 (ma_kh, ten_kh, vung, ngay_dh, doanh_so, loi_nhuan, so_luong)
VALUES
    ('KH01', 'Công ty Minh Phát',   'Miền Bắc',   '2026-01-15',  82000000, 24000000,  450),
    ('KH02', 'Công ty Hòa Bình',    'Miền Nam',   '2026-02-12',  96000000, 31000000,  510),
    ('KH03', 'Tập đoàn Đông Á',     'Miền Trung', '2026-03-18', 105000000, 35000000,  580),
    ('KH04', 'Công ty Tân Tiến',    'Miền Bắc',   '2026-04-09', 118000000, 42000000,  620),
    ('KH05', 'Công ty Phú Hưng',    'Miền Nam',   '2026-05-21', 126000000, 40000000,  690),
    ('KH06', 'Công ty Nam Việt',    'Miền Trung', '2026-06-14', 138000000, 48000000,  720),
    ('KH07', 'Công ty An Khang',    'Miền Bắc',   '2026-07-19', 148000000, 53000000,  780),
    ('KH08', 'Công ty Thành Đạt',   'Miền Nam',   '2026-08-25', 156000000, 55000000,  810),
    ('KH09', 'Công ty Hoàng Gia',   'Miền Bắc',   '2026-09-11', 172000000, 62000000,  890),
    ('KH10', 'Công ty Đại Dương',   'Miền Trung', '2026-10-16', 188000000, 68000000,  950),
    ('KH11', 'Công ty Sao Mai',     'Miền Nam',   '2026-11-20', 205000000, 75000000, 1040),
    ('KH12', 'Tập đoàn Việt Hưng',  'Miền Bắc',   '2026-12-10', 240000000, 92000000, 1200);

SELECT 'DAVS Sample Seed Ready' AS status;
