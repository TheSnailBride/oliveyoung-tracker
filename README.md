# Olive Young Tracker (올리브영 가격 추적기)

올리브영의 실시간 랭킹 및 제품 가격을 추적하고 시각화하는 웹 애플리케이션입니다.

## 🛠 기술 스택

### Backend
- **Java 17 / Spring Boot 3.2.2**
- **Spring Security & JWT**: 카카오 로그인 및 보안
- **Spring Data JPA**: 데이터베이스 연동
- **MySQL**: 데이터 저장
- **Python 3.x**: 웹 크롤링 (`requests`, `BeautifulSoup4`)

### Frontend
- **React (TypeScript)**
- **Vite**: 빌드 도구
- **CSS**: 스타일링
- **Chart.js**: 가격 변동 차트 시각화
- **Lucide React**: 아이콘

## 🚀 주요 기능

1. **카카오 소셜 로그인**: 쉽고 빠른 회원가입 및 로그인
2. **실시간 랭킹 크롤링**: 파이썬 기반의 고속 크롤러를 통한 올리브영 랭킹 정보 수집
3. **가격 추적**: 제품별 가격 변동 이력 기록 및 시각화
4. **반응형 UI**: 모바일 및 데스크탑 최적화 화면

## 🏗 프로젝트 구조

```text
oliveyoung-tracker/
├── src/main/java/...      # Spring Boot 백엔드 소스
├── scraper.py             # 파이썬 크롤링 스크립트
├── frontend/              # React 프론트엔드 소스
│   ├── src/pages/         # 페이지 컴포넌트 (Login, List, Detail)
│   └── src/components/    # 공통 컴포넌트 (Header, BottomNav)
└── requirements.txt       # 파이썬 의존성
```

## ⚙️ 실행 방법

### 1. Backend & Crawler
```bash
./gradlew bootRun
```
*주의: Python 3.x와 `requests`, `beautifulsoup4` 패키지가 설치되어 있어야 합니다.*

### 2. Frontend
```bash
cd frontend
npm install
npm run dev
```