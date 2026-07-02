# Nihongo Listen

Học nghe tiếng Nhật qua video YouTube có transcript sync, tra từ inline (JMdict) và luyện SRS.

📖 **Hướng dẫn chạy web + crawl video (URL / từ khóa): [docs/HUONG-DAN.md](docs/HUONG-DAN.md)**

## Kiến trúc

| Service  | Tech                                      | Port (dev) |
|----------|-------------------------------------------|------------|
| backend  | Spring Boot 3.3 / Java 21                 | 8080       |
| crawler  | Python 3.11 / FastAPI                     | 8000       |
| frontend | React 18 / Vite / TypeScript / Tailwind   | 5173       |
| postgres | PostgreSQL 16                             | 5432       |
| redis    | Redis 7                                   | 6379       |
| minio    | MinIO (object storage, dùng sau)          | 9000/9001  |

## Chạy dev

### 1. Hạ tầng (Postgres + Redis + MinIO)

```bash
cp .env.example .env   # rồi sửa các secret
docker compose up -d
```

### 2. Backend

Cần JDK 21 + Maven (hoặc chạy bằng Docker nếu chưa cài):

```bash
cd backend
mvn spring-boot:run
# hoặc: docker build -t nihongo-backend . && docker run --rm -p 8080:8080 --env-file ../.env nihongo-backend
```

Flyway tự chạy migration khi khởi động. API tại `http://localhost:8080/api`.

### 3. Crawler

```bash
cd crawler
python -m venv .venv && .venv/Scripts/activate   # Windows
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite proxy `/api` → `http://localhost:8080`.

## Luồng integration (đã wire)

1. Admin gọi `POST /api/admin/crawl/trigger` (JWT role ADMIN) với `{"youtubeId": "..."}`.
2. Backend gọi crawler `POST /fetch-transcript` kèm header `X-Internal-Secret`.
3. Crawler lấy metadata (yt-dlp) + transcript (youtube-transcript-api), POST về backend `/internal/transcripts` kèm cùng secret.
4. Backend lưu `videos` + `transcripts` + `transcript_segments` (status `PENDING_REVIEW`).

## Test nhanh

```bash
# Đăng ký
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"a@b.com","password":"secret123","displayName":"Test"}'

# Đăng nhập → lấy accessToken
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"a@b.com","password":"secret123"}'

# Health crawler
curl http://localhost:8000/health
```

## Chạy production

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Nginx serve frontend tĩnh và proxy `/api` → backend.

## Cấu trúc

```
nihongo-listen/
├── backend/    # Spring Boot: auth, video, transcript, crawler orchestrator
├── crawler/    # FastAPI: yt-dlp + youtube-transcript-api
├── frontend/   # React + Vite
├── nginx/
├── docker-compose.yml        # dev: chỉ hạ tầng
└── docker-compose.prod.yml   # full stack
```
