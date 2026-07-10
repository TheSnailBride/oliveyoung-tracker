# OliveYoung Tracker

올리브영 상품 가격을 주기적으로 수집하고, 할인 및 가격 변동을 조회하는 풀스택 가격 추적 서비스입니다.

> Spring Boot API, Python crawler, Redis lock/cache, MySQL price history, React frontend를 연결해
> "상품 가격이 언제 싸졌는지"를 자동으로 추적할 수 있게 만든 개인 포트폴리오 프로젝트입니다.

## 포트폴리오 요약

| 항목 | 내용 |
| --- | --- |
| 프로젝트 유형 | 커머스 가격 추적 서비스 |
| 담당 범위 | 백엔드 API, 인증, 크롤러 실행 orchestration, 데이터 모델링, 프론트엔드 조회 화면 |
| 핵심 문제 | 상품 가격은 계속 변하고, 크롤링은 중복 실행되기 쉬우며, 오래된 상품 데이터는 검색 품질을 떨어뜨림 |
| 해결 방향 | 스케줄 기반 자동 수집, Redis 분산 락, 일별 최저가 이력, stale 상품 관리 정책으로 운영 흐름 구성 |
| 검증 범위 | 백엔드 테스트 13개, 프론트엔드 테스트 3개, Python syntax check |

## 핵심 성과

- **자동 수집 파이프라인 구성**: Spring Boot 스케줄러가 매일 `03:00`에 Python 크롤러를 실행하고, 수집 결과를 내부 API로 다시 적재합니다.
- **중복 크롤링 방지**: Redis `SET NX` 기반 락과 `5분` TTL로 수동 실행과 자동 실행이 겹치는 상황을 막았습니다.
- **가격 이력 정규화**: 같은 상품의 하루 가격 이력은 여러 건을 무작정 저장하지 않고, 일별 최저가 1건 중심으로 보존하도록 설계했습니다.
- **오래된 데이터 관리**: `30일` 이상 재확인되지 않은 상품은 품절 처리하고, `90일` 이상 미확인 상품은 삭제 후보 기준으로 분리했습니다.
- **조회 API 확장**: 상품 검색, 할인율 높은 상품, 가격 하락 상품, 역대 최저가, 가격 차트, 유사 상품, 통계 등 주요 API 13개를 구성했습니다.
- **보안 경계 분리**: 사용자 인증은 Kakao OAuth + JWT로 처리하고, 크롤러 내부 API는 별도 `X-Crawler-Token`으로 보호했습니다.

## 기술 선택 이유

| 기술 | 선택 이유 |
| --- | --- |
| Spring Boot | REST API, Security, Scheduling, JPA를 한 애플리케이션 안에서 안정적으로 구성하기 위해 선택했습니다. 크롤러 실행과 상품 API를 같은 도메인 흐름으로 묶기 좋았습니다. |
| MySQL | 상품, 가격 이력, 사용자처럼 관계가 명확한 데이터를 다루고, 검색/정렬/집계 조건이 많은 커머스 도메인에 적합하다고 판단했습니다. |
| Redis | 상품 조회 캐시와 크롤러 중복 실행 방지 락처럼 빠른 key-value 접근이 필요한 영역에 사용했습니다. |
| Python crawler | 웹 수집 라이브러리 생태계가 풍부하고, HTML 구조 변화에 빠르게 대응하며 크롤링 로직을 반복 개선하기 쉬워 선택했습니다. |
| React + TypeScript | 가격 차트, 필터, 상세 페이지처럼 상태가 있는 조회 UI를 타입 안정성 있게 구성하기 위해 사용했습니다. |
| H2 Test | MySQL 의존도를 낮추고 Spring context/JPA 테스트를 빠르게 돌릴 수 있도록 테스트 환경을 분리했습니다. |

## 문제 해결 포인트

- **크롤러 중복 실행 문제**: 수동 API 호출과 자동 스케줄이 동시에 실행될 수 있어 Redis 락을 두고, 이미 실행 중이면 `409 Conflict`로 거절하도록 처리했습니다.
- **가격 이력 과다 적재 문제**: 크롤러가 같은 상품을 반복 확인하더라도 차트용 가격 이력이 불필요하게 늘어나지 않도록 일별 최저가 중심으로 저장했습니다.
- **검색 품질 저하 문제**: 오래 수집되지 않은 상품이 계속 노출되지 않도록 `lastSeenAt` 기준의 품절 처리 정책을 넣었습니다.
- **내부 API 노출 문제**: 크롤러 import/API 조회 경로는 일반 사용자 인증과 분리해 내부 토큰이 있어야 접근할 수 있게 했습니다.
- **로컬 실행 안정성 문제**: `.env` 기반 설정, Redis 컨테이너, MySQL/H2 테스트 분리를 통해 실행 환경과 테스트 환경을 나눴습니다.

## 프로젝트 개요

- 상품 목록, 상세 가격 추이, 할인 상품, 최저가 상품을 조회합니다.
- 카카오 OAuth 로그인 후 JWT로 인증합니다.
- Spring Boot 서버가 Python 크롤러를 내부 스케줄로 실행합니다.
- Redis로 상품 조회 캐시와 크롤러 중복 실행 방지 락을 관리합니다.
- 오래된 가격 이력과 장기간 미확인 상품을 자동 정리합니다.

## 기술 스택

| 영역 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 3.2.3, Spring Web, Spring Security, Spring Data JPA |
| Database | MySQL 8, H2 Test |
| Cache/Lock | Redis |
| Auth | JWT, Kakao OAuth |
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

### 자동 크롤링

- 기본 실행 시간: 매일 03:00, `Asia/Seoul`
- Spring Boot의 `CrawlerScheduler`가 `scraper.py`를 `ProcessBuilder`로 실행합니다.
- Python 크롤러는 내부 API를 호출해 기존 상품 목록을 가져오고, 수집 결과를 `/api/crawler/import`로 전송합니다.
- 크롤러 내부 API는 `X-Crawler-Token`으로 보호됩니다.
- Redis 락으로 수동/자동 크롤러 중복 실행을 방지합니다.

### 상품 유지보수

- 기본 실행 시간: 매일 03:30, `Asia/Seoul`
- 6개월 이상 지난 가격 이력은 삭제합니다.
- 30일 이상 크롤러에서 다시 확인되지 않은 상품은 `isSoldOut = true`로 표시합니다.
- 90일 이상 미확인 상품은 실제 삭제 후보로 보되, 가격 이력 보존 정책을 정한 뒤 별도 처리합니다.

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
│   ├── domain/product/          # product and price history domain
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
| `JWT_SECRET` | empty | JWT 서명 키. 운영에서는 반드시 긴 랜덤 문자열로 설정 |
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
| `BACKEND_INTERNAL_BASE_URL` | `http://localhost:${SERVER_PORT}/api` | Python 크롤러가 호출할 내부 API base URL |

## 로컬 실행

### 1. Redis 실행

```bash
docker-compose up -d redis
```

Redis는 기본적으로 `127.0.0.1:16379`에 바인딩됩니다. `.env`에서 `REDIS_PORT`를 바꾸면 해당 포트로 실행됩니다.

Docker Compose v2가 설치된 환경에서는 다음 명령도 사용할 수 있습니다.

```bash
docker compose up -d redis
```

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
npm run test:categories
npm run build
```

PowerShell에서는 `npm.cmd`를 사용할 수 있습니다.

```powershell
npm.cmd run test:products
npm.cmd run test:categories
npm.cmd run build
```

## 운영 메모

- `CRAWLER_INTERNAL_TOKEN`은 외부에 노출되지 않는 긴 랜덤 문자열로 설정합니다.
- 운영 DB에서 `ddl-auto: update`를 계속 사용할지 여부는 배포 단계에서 별도 판단이 필요합니다.
- 현재 오래 미확인된 상품은 품절 처리까지만 수행합니다. 실제 삭제는 가격 이력 보존 정책을 정한 뒤 별도 작업으로 진행합니다.
- 크롤러가 실패하면 상품의 `lastSeenAt`이 갱신되지 않으므로, 연속 실패가 길어질 경우 30일 stale 기준에 영향을 줄 수 있습니다.
