# Similar Products Recommendation Design / 유사 상품 추천 기능 설계

## Objective (목표)
Provide product recommendations at the bottom of the Product Detail page. 
(상품 상세 페이지 하단에 추천 상품 목록을 제공합니다.)

## Approach (접근 방식)
Currently implementing **Approach #1: Same Category + Same Brand** to suggest direct alternatives (like different sizes/volumes of the exact same product type from the same brand). 
The architecture will be designed to easily accommodate **Approach #2 (Same Category + Different Brand)** in the future.
(현재는 **방식 #1: 같은 카테고리 + 같은 브랜드**를 우선 구현하여, 같은 브랜드의 용량만 다른 상품이나 직접적인 대체재를 제안합니다. 향후 **방식 #2: 같은 카테고리 + 다른 브랜드** 추천도 쉽게 추가할 수 있도록 확장성을 고려하여 아키텍처를 설계합니다.)

## Architecture & Data Flow (아키텍처 및 데이터 흐름)

### Backend (`ProductController` & `ProductService`)
- **New Endpoint (신규 API):** `GET /api/v1/products/{productId}/similar`
- **Query Logic (조회 로직):** 
  - Find products where `category == target.category` AND `brand == target.brand`. (타겟 상품과 카테고리 및 브랜드가 일치하는 상품 조회)
  - Exclude the `target` product itself from the results. (결과에서 현재 보고 있는 타겟 상품 본인은 제외)
  - Limit the results to a reasonable number (e.g., 4 or 5 items). (보여줄 상품 개수를 4~5개 정도로 제한)
- **Future Extensibility (향후 확장성):** The service method will accept parameters (or a criteria object) so we can easily add a "Same Category, Any Brand" fetcher method later without rewriting the core logic. (서비스 메서드에 조건을 파라미터로 넘길 수 있도록 설계하여, 나중에 핵심 로직을 뜯어고치지 않고도 "같은 카테고리의 다른 브랜드 인기상품"을 조회할 수 있게 만듭니다.)

### Frontend (`ProductDetail.tsx`)
- **UI Component (UI 컴포넌트):** A new section below the price history chart titled "이 브랜드의 비슷한 상품" (Similar products from this brand). (가격 추이 차트 아래에 "이 브랜드의 비슷한 상품"이라는 새로운 섹션 추가)
- **Layout (레이아웃):** A responsive grid or horizontal scroll showing the product image, name, and current price. Clicking a recommended product navigates to its detail page. (상품 이미지, 이름, 현재 가격을 보여주는 반응형 그리드 또는 가로 스크롤 형태. 상품 클릭 시 해당 상품의 상세 페이지로 이동)
- **State (상태 관리):** Fetch the similar products on component mount or when `productId` changes. (컴포넌트가 마운트되거나 `productId`가 변경될 때 유사 상품 데이터를 다시 불러옴)

## Error Handling & Edge Cases (예외 처리)
- **No Similar Products (유사 상품이 없을 때):** If the query returns 0 results, the section should be hidden gracefully so the UI doesn't look empty. (조회 결과가 0개일 경우, 화면이 비어 보이지 않도록 해당 섹션을 자연스럽게 숨김 처리)
- **Loading State (로딩 상태):** Show skeleton loaders or a spinner while the similar products are being fetched. (데이터를 불러오는 동안 스켈레톤 UI나 스피너 표시)

## Testing Strategy (테스트 전략)
- Unit test for `ProductRepository` to ensure the correct products (excluding self) are returned. (`ProductRepository`에서 본인을 제외한 알맞은 상품들이 잘 조회되는지 단위 테스트)
- Integration test for the `GET` endpoint. (`GET` API 엔드포인트 통합 테스트)
