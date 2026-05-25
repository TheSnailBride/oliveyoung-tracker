# 🚀 올리브영 가격 추적 서비스 (OliveYoung Tracker)

> **"사용자가 원하는 상품의 가격 변동을 추적하고, 목표가 도달 시 카카오톡 알림을 제공하는 모니터링 시스템"**

## 1. 프로젝트 개요
- **진행 기간:** 202X.XX ~ 202X.XX
- **개발 인원:** 1인 (또는 프론트1, 백엔드1)
- **주요 역할 (백엔드):** API 서버 설계 및 개발, DB 모델링, 크롤링 자동화 파이프라인 구축, 카카오 OAuth 및 알림 연동
- **GitHub:** [레포지토리 링크 삽입]

## 2. 사용 기술 (Tech Stack)
- **Backend:** Java 17, Spring Boot 3.2.3, Spring Data JPA, Spring Security
- **Database:** MySQL 8, H2 (Test), Redis
- **Scraping:** Python 3 (Requests, BeautifulSoup4)
- **Auth & API:** JWT, Kakao Login API, Kakao Message API
- **Frontend:** React (TypeScript), Vite, Chart.js

## 3. 시스템 아키텍처
- **Client (React) ↔ Spring Boot API Server ↔ MySQL Database**
- **Spring Boot API Server ↔ Python Crawler (스케줄링 기반 크롤링 실행 및 데이터 파싱)**
- **Spring Boot API Server ↔ Redis (조회 캐시 및 크롤러 중복 실행 방지 락)**

## 4. 주요 기능
- **사용자 인증:** 카카오 소셜 로그인 및 JWT 기반 인가 (Access Token 24시간)
- **상품 및 가격 추적:** 사용자가 등록한 상품의 가격 변동 내역(Price History) 저장
- **알림 시스템:** 상품 가격이 설정한 목표가 이하로 떨어지면 카카오톡 메시지 알림 전송
- **크롤링 실행:** Spring Boot 실행 후 수동 API 요청으로 파이썬 크롤러 실행

---

## 🔥 5. 핵심 기술적 고민 및 문제 해결 경험 (Troubleshooting)

### 🛠 Issue 1: 크롤링 성능 향상 및 Anti-Bot 우회 전략
- **배경:** 초기에 Java의 JSoup이나 Selenium을 사용하려 했으나, 동적 렌더링 페이지 처리와 차단(Anti-bot) 이슈, 그리고 무거운 브라우저 렌더링으로 인한 리소스 낭비 문제가 발생했습니다.
- **해결 과정:** 
  - 크롤링 로직을 Java 내부에 결합하지 않고, 빠르고 유연한 **Python 스크립트 기반 아키텍처로 분리**했습니다 (`requests`, `beautifulsoup4` 활용).
  - Spring의 `ProcessBuilder`를 통해 외부 Python 스크립트를 호출하고 JSON으로 파싱하여 응답받는 형태로 개선했습니다.
- **결과:** 크롤링 속도를 최적화하고 서버 메모리 사용량을 절감했습니다. 또한 향후 크롤링 로직 변경 시 API 서버 재배포 없이 스크립트만 수정할 수 있도록 모듈 결합도를 낮추었습니다.

### 🛠 Issue 2: 이메일 대신 카카오톡 메시지를 활용한 알림 도달률 개선
- **배경:** 목표가 도달 시 사용자에게 알림을 주어야 하는데, 기존 이메일 알림 방식은 사용자의 확인 속도가 느려 실시간성이 떨어졌습니다.
- **해결 과정:**
  - 사용자 인증 단계에서 도입한 **Kakao OAuth의 Access Token을 재활용**하여, 카카오톡 메시지 API(Kakao Message API)로 알림 수단을 전면 교체했습니다.
- **결과:** 사용자의 모바일 기기로 즉각적인 알림이 가능해져 서비스의 핵심 가치인 '실시간 가격 추적' 기능을 크게 향상시켰습니다.

### 🛠 Issue 3: JWT + Spring Security를 활용한 안전한 인증 인프라 구축
- **배경:** 무상태(Stateless) 서버를 유지하면서 소셜 로그인을 매끄럽게 처리해야 했습니다.
- **해결 과정:** 
  - `JwtAuthenticationFilter`를 구현하여 모든 API 요청 시 토큰의 유효성을 검증했습니다.
  - 카카오로부터 사용자 정보를 받아 서비스 자체 `User` 엔티티로 변환 후, 커스텀 JWT 토큰을 발급하는 프로세스를 설계했습니다.
- **결과:** 세션 저장소를 따로 두지 않아 서버 확장성에 유리한 구조를 완성했습니다.

### 🛠 Issue 4: 가격 변동 데이터(Price History)의 효율적인 관리
- **배경:** 스케줄러(`CrawlerScheduler`)가 주기적으로 수많은 상품의 가격을 업데이트합니다. 가격이 변경되지 않았는데도 무의미하게 데이터가 적재되는 문제가 발생할 수 있었습니다.
- **해결 과정:**
  - JPA를 활용하여 업데이트 직전의 최신 가격을 조회하고, **기존 가격과 변동이 있을 때만 새로운 `PriceHistory` 레코드를 Insert** 하도록 로직을 최적화했습니다.
- **결과:** 불필요한 데이터베이스 공간 낭비를 막고 쿼리 성능 저하를 방지했습니다.

---

## 🏗 6. 프로젝트 구조 (디렉토리 구조)

```text
oliveyoung-tracker/
├── src/main/java/...      # Spring Boot 백엔드 소스
├── scraper.py             # 파이썬 크롤링 스크립트
├── frontend/              # React 프론트엔드 소스
│   ├── src/pages/         # 페이지 컴포넌트 (Login, List, Detail)
│   └── src/components/    # 공통 컴포넌트 (Header, BottomNav)
└── requirements.txt       # 파이썬 의존성
```

## ⚙️ 7. 빌드 및 실행 방법

### Backend & Crawler
```bash
docker compose up -d redis
```

```bash
./gradlew bootRun
```
*주의: Redis는 기본적으로 `localhost:7080`을 사용합니다. Python 3.x 환경에 `requests`, `beautifulsoup4` 패키지가 설치되어 있어야 합니다. (`pip install -r requirements.txt`)*

### Frontend
```bash
cd frontend
npm install
npm run dev
```
