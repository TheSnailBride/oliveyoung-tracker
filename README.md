# OliveYoung Tracker

올리브영 상품 가격을 주기적으로 수집하고, 사용자가 설정한 목표가에 도달하면 카카오톡 알림을 보내는 가격 추적 서비스입니다.

## 프로젝트 개요

- 상품 목록, 상세 가격 추이, 할인 상품, 최저가 상품을 조회합니다.
- 카카오 OAuth 로그인 후 JWT로 인증합니다.
- 사용자는 상품별 목표가 알림을 설정할 수 있습니다.
- Spring Boot 서버가 Python 크롤러를 내부 스케줄로 실행합니다.
- Redis로 상품 조회 캐시와 크롤러 중복 실행 방지 락을 관리합니다.
- 오래된 가격 이력, 오래된 알림, 장기간 미확인 상품을 자동 정리합니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 3.2.3, Spring Web, Spring Security, Spring Data JPA |
| Database | MySQL 8, H2 Test |
| Cache/Lock | Redis |
| Auth/Notification | JWT, Kakao OAuth, Kakao Message API |
| Crawler | Python 3, requests, scrapling |
| Frontend | React 18, TypeScript, Vite, Chart.js |
| Test | JUnit 5, Spring Security Test, Node test runner |

## 주요 기능

### 상품 조회

- 키워드, 카테고리, 브랜드, 할인 여부 기반 상품 검색
- 할인율 높은 상품 조회
- 역대 최저가 도달 상품 조회
- 상품 상세 가격 차트
- 같은 브랜드/카테고리 기반 유사 상품 추천

### 가격 추적

- 크롤러가 상품을 다시 확인하면 `lastSeenAt`을 갱신합니다.
- 가격 이력은 하루에 무조건 여러 건을 쌓지 않고, 같은 날에는 최저가 1건만 유지합니다.
- 현재 가격은 최신 크롤링 결과로 갱신하고, 가격 이력은 일별 최저가 추이용으로 보존합니다.

### 목표가 알림

- 사용자가 상품별 목표가를 설정합니다.
- 현재가가 목표가 이하로 내려가면 알림 내역을 저장하고, 카카오 연동 사용자에게 메시지를 보냅니다.
- 목표가 도달 후 해당 알림 설정은 1회성으로 삭제됩니다.
- 설정한 지 6개월이 지난 목표가 알림은 유지보수 스케줄러가 자동 삭제합니다.

### 자동 크롤링

- 기본 실행 시간: 매일 03:00, `Asia/Seoul`
- Spring Boot의 `CrawlerScheduler`가 `scraper.py`를 `ProcessBuilder`로 실행합니다.
- Python 크롤러는 내부 API를 호출해 기존 상품 목록을 가져오고, 수집 결과를 `/api/crawler/import`로 전송합니다.
- 크롤러 내부 API는 `X-Crawler-Token`으로 보호됩니다.
- Redis 락으로 수동/자동 크롤러 중복 실행을 방지합니다.

### 상품 유지보수

- 기본 실행 시간: 매일 03:30, `Asia/Seoul`
- 6개월 이상 지난 가격 이력은 삭제합니다.
- 6개월 이상 지난 목표가 알림은 삭제합니다.
- 30일 이상 크롤러에서 다시 확인되지 않은 상품은 `isSoldOut = true`로 표시합니다.
- 90일 이상 미확인 상품은 실제 삭제 후보로 보되, 가격 이력과 알림 연결이 있으므로 현재는 hard delete하지 않습니다.

## 시스템 구조

```text
React Frontend
  -> Spring Boot API
      -> MySQL
      -> Redis cache / crawler lock
      -> Python crawler process
          -> Olive Young mobile pages
          -> Spring Boot internal crawler APIs
```

## 프로젝트 구조

```text
oliveyoung-tracker/
├── src/main/java/com/oliveyoung/tracker/
│   ├── config/                  # Security, cache, time config
│   ├── crawler/                 # crawler scheduler, lock, ingestion service
│   ├── domain/product/          # product, price history, alert, notification domain
│   └── domain/user/             # Kakao auth and user domain
├── src/main/resources/
│   ├── application.yml
│   └── application.yml.example
├── frontend/
│   ├── src/api/                 # frontend API helpers
│   ├── src/components/
│   ├── src/pages/
│   └── tests/
├── scraper.py                   # Python crawler
├── requirements.txt             # Python dependencies
└── docker-compose.yml           # local Redis
```

## 환경 변수

### Backend

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Spring Boot 서버 포트 |
| `DB_USERNAME` | `root` | MySQL 사용자 |
| `DB_PASSWORD` | `your_db_password` | MySQL 비밀번호 |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `16379` | Redis port |
| `REDIS_PASSWORD` | empty | Redis password |
| `JWT_SECRET` | example secret | JWT 서명 키 |
| `KAKAO_CLIENT_ID` | example value | Kakao REST API key |
| `KAKAO_CLIENT_SECRET` | example value | Kakao client secret |
| `KAKAO_REDIRECT_URI` | example value | Kakao callback URL |

### Crawler and Maintenance

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `CRAWLER_INTERNAL_TOKEN` | empty | 크롤러 내부 API 보호 토큰. 운영에서는 반드시 설정 |
| `CRAWLER_SCHEDULE_CRON` | `0 0 3 * * *` | 자동 크롤링 cron |
| `CRAWLER_SCHEDULE_ZONE` | `Asia/Seoul` | 자동 크롤링 timezone |
| `CRAWLER_LOCK_TTL` | `5m` | Redis 크롤러 락 TTL |
| `CRAWLER_LOCK_REDIS_KEY` | `lock:crawler:manual-run` | Redis 크롤러 락 key |
| `PRODUCT_MAINTENANCE_CRON` | `0 30 3 * * *` | 상품 유지보수 cron |
| `PRODUCT_MAINTENANCE_ZONE` | `Asia/Seoul` | 상품 유지보수 timezone |

## 로컬 실행

### 1. Redis 실행

```bash
docker compose up -d redis
```

Redis는 기본적으로 `127.0.0.1:16379`에 바인딩됩니다.

### 2. Python 의존성 설치

```bash
pip install -r requirements.txt
```

### 3. Backend 실행

```bash
./gradlew bootRun
```

Windows PowerShell에서는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat bootRun
```

### 4. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

PowerShell 실행 정책 때문에 `npm`이 막히면 `npm.cmd`를 사용합니다.

```powershell
npm.cmd run dev
```

## 수동 크롤링

서버 자동 스케줄이 기본 경로입니다. 수동 실행이 필요하면 내부 토큰을 헤더로 전달합니다.

```bash
curl -X POST http://localhost:8080/api/crawler/run \
  -H "X-Crawler-Token: your_crawler_internal_token"
```

Python 크롤러를 직접 실행할 때도 같은 토큰을 환경 변수로 전달합니다.

```bash
CRAWLER_INTERNAL_TOKEN=your_crawler_internal_token python scraper.py
```

## 검증 명령

### Backend

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

### Python

```bash
python -m py_compile scraper.py
```

### Frontend

```bash
cd frontend
npm run test:products
npm run test:product-alerts
npm run test:notifications
npm run test:categories
npm run build
```

PowerShell에서는 `npm.cmd`를 사용할 수 있습니다.

```powershell
npm.cmd run test:products
npm.cmd run test:product-alerts
npm.cmd run test:notifications
npm.cmd run test:categories
npm.cmd run build
```

## 운영 메모

- `CRAWLER_INTERNAL_TOKEN`은 외부에 노출되지 않는 긴 랜덤 문자열로 설정합니다.
- 운영 DB에서 `ddl-auto: update`를 계속 사용할지 여부는 배포 단계에서 별도 판단이 필요합니다.
- 현재 오래 미확인된 상품은 품절 처리까지만 수행합니다. 실제 삭제는 가격 이력, 목표가 알림, 알림 내역 보존 정책을 정한 뒤 별도 작업으로 진행합니다.
- 크롤러가 실패하면 상품의 `lastSeenAt`이 갱신되지 않으므로, 연속 실패가 길어질 경우 30일 stale 기준에 영향을 줄 수 있습니다.
