# Hướng dẫn chạy Nihongo Listen & crawl video

> Tài liệu cho môi trường dev trên Windows. Mọi lệnh chạy trong thư mục gốc
> `D:\Web học tiếng Nhật\nihongo-listen` trừ khi ghi khác.

## 1. Yêu cầu môi trường

| Công cụ | Bắt buộc | Ghi chú |
|---|---|---|
| Docker Desktop | ✅ | Chạy Postgres/Redis/MinIO và build backend |
| Node.js 20+ | ✅ | Frontend |
| Python 3.11 | ✅ | Crawler |
| JDK 21 + Maven | ❌ | Chỉ cần nếu muốn chạy backend không qua Docker |

## 2. Chạy web (4 bước, theo đúng thứ tự)

### Bước 1 — Hạ tầng (Postgres + Redis + MinIO)

```powershell
docker compose up -d
```

Kiểm tra: `docker ps` phải thấy `nihongo-postgres`, `nihongo-redis`, `nihongo-minio` đang `Up`.

### Bước 2 — Backend (Spring Boot, port 8080)

Cách A — qua Docker (khuyên dùng vì máy chưa cài JDK):

```powershell
docker build -t nihongo-backend .\backend
docker rm -f nihongo-backend 2>$null
docker run -d --name nihongo-backend --network nihongo-listen_default -p 8080:8080 `
  -e DATABASE_URL="jdbc:postgresql://postgres:5432/nihongo" `
  -e DATABASE_USER=nihongo -e DATABASE_PASSWORD=change-me `
  -e REDIS_URL="redis://redis:6379" `
  -e CRAWLER_BASE_URL="http://host.docker.internal:8000" `
  nihongo-backend
```

Cách B — nếu đã cài JDK 21 + Maven:

```powershell
cd backend
mvn spring-boot:run
```

Kiểm tra: mở http://localhost:8080/api/videos → thấy JSON `{"content":[...]}`.
Flyway tự tạo/cập nhật schema DB khi backend khởi động — không cần chạy SQL tay.

### Bước 3 — Crawler (FastAPI, port 8000)

Lần đầu tiên cần tạo venv:

```powershell
cd crawler
python -m venv .venv
.\.venv\Scripts\pip install -r requirements.txt
```

Chạy (các lần sau chỉ cần lệnh này):

```powershell
cd crawler
.\.venv\Scripts\python -m uvicorn app.main:app --port 8000
```

Kiểm tra: http://localhost:8000/health → `{"status":"ok"}`.

### Bước 4 — Frontend (Vite, port 5173)

```powershell
cd frontend
npm install     # chỉ lần đầu
npm run dev
```

Mở **http://localhost:5173** — Vite tự proxy `/api` sang backend 8080.

## 3. Tài khoản

- Đăng ký tài khoản mới ngay trên web (role mặc định `FREE`).
- Tài khoản admin có sẵn từ lúc dev: **test@example.com / password123**.
- Muốn cấp quyền admin cho tài khoản khác:

```powershell
docker exec nihongo-postgres psql -U nihongo -d nihongo -c "UPDATE users SET role='ADMIN' WHERE email='ban@email.com';"
```

## 4. Crawl video từ YouTube

Cả hai cách đều là API admin — cần đăng nhập lấy `accessToken` trước.
Video crawl về có status `PENDING_REVIEW`, **phải duyệt (mục 5) mới hiện lên web**.

Điều kiện video crawl được: có phụ đề **tiếng Nhật** (ưu tiên phụ đề chuẩn do người
đăng tạo, fallback phụ đề tự động), độ dài 3–30 phút (chỉ áp dụng khi crawl theo từ khóa).

### ⚠️ Lưu ý encoding trên Windows

Đừng gõ từ khóa tiếng Nhật/tiếng Việt trực tiếp trong lệnh `curl` ở CMD — console
sẽ biến chữ thành `?????` trước khi gửi đi. Dùng một trong các cách dưới đây
(PowerShell `Invoke-RestMethod` với `charset=utf-8`, hoặc script Python có sẵn).

### Lấy token (PowerShell)

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType "application/json; charset=utf-8" `
  -Body '{"email":"test@example.com","password":"password123"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
```

### Cách 1 — Crawl bằng URL (hoặc video ID)

Dán URL YouTube bất kỳ dạng nào: `watch?v=`, `youtu.be/`, `/shorts/`, `/embed/`, `/live/`.

```powershell
$body = @{ url = "https://www.youtube.com/watch?v=4K10HB3-bdk" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/admin/crawl/trigger `
  -Headers $headers -ContentType "application/json; charset=utf-8" -Body $body
```

Hoặc dùng thẳng video ID: `$body = @{ youtubeId = "4K10HB3-bdk" } | ConvertTo-Json`.

Kết quả trả về: số segments đã lấy + `videoId` trong DB. Nếu video không có phụ đề
tiếng Nhật sẽ báo lỗi 502 kèm lý do.

### Cách 2 — Crawl theo từ khóa

Tìm kiếm YouTube bằng yt-dlp (không cần YouTube API key), tự lọc video 3–30 phút
có phụ đề tiếng Nhật rồi crawl. `maxResults` = số video muốn lấy (1–10, mặc định 3).
Chạy tuần tự từng video nên có thể mất vài phút.

```powershell
$body = @{ keyword = "日本語 リスニング N5 会話"; maxResults = 3 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/admin/crawl/search `
  -Headers $headers -ContentType "application/json; charset=utf-8" -Body $body
```

Response cho biết: `candidatesFound` (số video tìm thấy), `ingested` (đã crawl thành công),
`skipped` (bị loại + lý do: không có phụ đề ja / độ dài ngoài khoảng).

Gợi ý từ khóa tốt: `日本語 リスニング N5`, `やさしい日本語 ニュース`, `日本語 会話 初級`,
`JLPT N4 聴解`, `japanese listening practice N3`.

Cách khác: sửa từ khóa trong `crawler/test_search.py` rồi chạy
`.\.venv\Scripts\python test_search.py "từ khóa"` (trong thư mục `crawler`).

## 5. Duyệt video (bắt buộc trước khi hiện lên web)

Xem hàng chờ duyệt:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/videos?status=PENDING_REVIEW" -Headers $headers
```

Duyệt / loại (thay `{id}` bằng id trong kết quả trên):

```powershell
Invoke-RestMethod -Method Put -Uri http://localhost:8080/api/admin/videos/{id}/approve -Headers $headers
Invoke-RestMethod -Method Put -Uri http://localhost:8080/api/admin/videos/{id}/reject  -Headers $headers
```

Sau khi approve, refresh http://localhost:5173 → video xuất hiện, click vào để xem
player + transcript đồng bộ (click câu để nhảy tới timestamp).

## 6. Lỗi thường gặp

| Triệu chứng | Nguyên nhân / cách sửa |
|---|---|
| `localhost:5173` không vào được | Frontend chưa chạy → `cd frontend; npm run dev` |
| Trang list báo "Backend đã chạy chưa?" | Backend chưa chạy hoặc chưa xong khởi động → xem `docker logs nihongo-backend` |
| Crawl báo 502 "Crawler service không phản hồi" | Crawler (port 8000) chưa chạy → bước 3 |
| Crawl báo "không có transcript tiếng Nhật" | Video đó không có phụ đề ja — chọn video khác |
| Từ khóa tìm ra kết quả lung tung | Từ khóa bị mangle encoding — xem mục ⚠️ ở trên |
| Backend không kết nối được DB | Hạ tầng chưa chạy → `docker compose up -d`, đợi postgres `healthy` |
| Video crawl xong không thấy trên web | Chưa approve — xem mục 5 |

## 7. Tắt mọi thứ

```powershell
docker rm -f nihongo-backend      # backend
docker compose down               # hạ tầng (giữ data trong volume)
# frontend/crawler: Ctrl+C trong terminal đang chạy
```
