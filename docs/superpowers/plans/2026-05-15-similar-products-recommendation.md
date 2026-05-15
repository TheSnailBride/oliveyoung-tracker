# Similar Products Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a "Similar Products" section at the bottom of the Product Detail page, suggesting other items from the same brand and category.

**Architecture:** We will add a new Spring Data JPA repository method to find matching products (excluding the current one), expose this via a new GET endpoint in the `ProductController`, and update the React frontend to fetch and display this data in the `ProductDetail` component.

**Tech Stack:** Java 17, Spring Boot, Spring Data JPA, React, TypeScript.

---

### Task 1: Extend ProductRepository with Similar Products Query

**Files:**
- Modify: `src/main/java/com/oliveyoung/tracker/domain/product/repository/ProductRepository.java`
- Create: `src/test/java/com/oliveyoung/tracker/domain/product/repository/ProductRepositoryTest.java`

- [ ] **Step 1: Write the repository test**
Create `ProductRepositoryTest.java` to test the new query method.

```java
package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("같은 브랜드와 카테고리를 가지며 특정 ID가 아닌 상품들을 제한된 개수로 조회한다")
    void findTop5ByCategoryAndBrandAndIdNot() {
        // given
        Product target = productRepository.save(Product.builder()
                .oliveYoungId("A001")
                .name("타겟 상품")
                .category("스킨케어")
                .brand("A브랜드")
                .build());

        Product similar1 = productRepository.save(Product.builder()
                .oliveYoungId("A002")
                .name("유사 상품 1")
                .category("스킨케어")
                .brand("A브랜드")
                .build());

        Product similar2 = productRepository.save(Product.builder()
                .oliveYoungId("A003")
                .name("다른 카테고리")
                .category("바디케어")
                .brand("A브랜드")
                .build());

        // when
        List<Product> results = productRepository.findTop5ByCategoryAndBrandAndIdNot(
                "스킨케어", "A브랜드", target.getId());

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("유사 상품 1");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew test --tests "*ProductRepositoryTest*"`
Expected: Compilation failure because `findTop5ByCategoryAndBrandAndIdNot` is not defined.

- [ ] **Step 3: Write minimal implementation**
Modify `ProductRepository.java` to add the new method.

```java
package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByOliveYoungId(String oliveYoungId);
    List<Product> findByOliveYoungIdIn(List<String> oliveYoungIds);
    Page<Product> findByCategory(String category, Pageable pageable);
    Page<Product> findByBrand(String brand, Pageable pageable);

    // 새롭게 추가되는 메서드
    List<Product> findTop5ByCategoryAndBrandAndIdNot(String category, String brand, Long id);

    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.brand LIKE %:keyword%) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:isSale IS NULL OR p.isSale = :isSale)")
    Page<Product> searchProducts(String keyword, String category, Boolean isSale, Pageable pageable);
}
```

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew test --tests "*ProductRepositoryTest*"`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add src/main/java/com/oliveyoung/tracker/domain/product/repository/ProductRepository.java src/test/java/com/oliveyoung/tracker/domain/product/repository/ProductRepositoryTest.java
git commit -m "feat(backend): add query for similar products in ProductRepository"
```

---

### Task 2: Implement ProductService Logic for Similar Products

**Files:**
- Modify: `src/main/java/com/oliveyoung/tracker/domain/product/service/ProductService.java`

- [ ] **Step 1: Write the implementation**
Open `ProductService.java` and add the `getSimilarProducts` method to retrieve matching products and convert them to `ProductResponse` DTOs.

```java
    // 기존 메서드들 아래에 추가
    @Transactional(readOnly = true)
    public List<ProductResponse> getSimilarProducts(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));

        List<Product> similarProducts = productRepository.findTop5ByCategoryAndBrandAndIdNot(
                product.getCategory(), 
                product.getBrand(), 
                product.getId()
        );

        return similarProducts.stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }
```
*Note: Make sure `import java.util.stream.Collectors;` is present at the top.*

- [ ] **Step 2: Commit**
```bash
git add src/main/java/com/oliveyoung/tracker/domain/product/service/ProductService.java
git commit -m "feat(backend): add getSimilarProducts method to ProductService"
```

---

### Task 3: Expose Similar Products API Endpoint

**Files:**
- Modify: `src/main/java/com/oliveyoung/tracker/domain/product/controller/ProductController.java`

- [ ] **Step 1: Write the implementation**
Open `ProductController.java` and add the new endpoint mapping.

```java
    // ProductController 클래스 내부에 추가
    @GetMapping("/{productId}/similar")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getSimilarProducts(
            @PathVariable Long productId) {
        List<ProductResponse> similarProducts = productService.getSimilarProducts(productId);
        return ResponseEntity.ok(ApiResponse.success(similarProducts));
    }
```

- [ ] **Step 2: Verify application builds**
Run: `./gradlew classes`
Expected: Build SUCCESS

- [ ] **Step 3: Commit**
```bash
git add src/main/java/com/oliveyoung/tracker/domain/product/controller/ProductController.java
git commit -m "feat(backend): expose GET /api/v1/products/{productId}/similar endpoint"
```

---

### Task 4: Frontend UI for Similar Products

**Files:**
- Modify: `frontend/src/pages/ProductDetail.tsx`

- [ ] **Step 1: Write the implementation**
Modify `ProductDetail.tsx` to fetch and render the similar products. We will add a new state `similarProducts` and a `useEffect` hook.

Add to imports and state declarations:
```typescript
// 상단 임포트에 추가 또는 기존 코드 활용
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
// 기존 코드 유지...

// 인터페이스 정의 (없다면 추가, ProductDetailResponse 등과 맞춤)
interface Product {
  id: number;
  name: string;
  brand: string;
  imageUrl: string;
  currentPrice: number;
  originalPrice: number;
  discountRate: number;
  isSale: boolean;
}

// 컴포넌트 내부 State 추가
  const [similarProducts, setSimilarProducts] = useState<Product[]>([]);
  const [loadingSimilar, setLoadingSimilar] = useState<boolean>(true);
  const navigate = useNavigate();
```

Add the fetch logic:
```typescript
  // useEffect 안에 혹은 별도의 useEffect로 추가
  useEffect(() => {
    if (!id) return;
    
    const fetchSimilarProducts = async () => {
      setLoadingSimilar(true);
      try {
        const response = await fetch(`/api/v1/products/${id}/similar`);
        const data = await response.json();
        if (data.success) {
          setSimilarProducts(data.data);
        }
      } catch (error) {
        console.error('Failed to fetch similar products:', error);
      } finally {
        setLoadingSimilar(false);
      }
    };

    fetchSimilarProducts();
  }, [id]);
```

Add the UI rendering at the bottom of the component return statement (just above the closing container div):
```tsx
      {/* 비슷한 상품 추천 섹션 */}
      {!loadingSimilar && similarProducts.length > 0 && (
        <div className="mt-12 mb-8">
          <h2 className="text-xl font-bold mb-4">이 브랜드의 비슷한 상품</h2>
          <div className="flex overflow-x-auto space-x-4 pb-4 snap-x">
            {similarProducts.map((prod) => (
              <div 
                key={prod.id} 
                className="flex-none w-40 cursor-pointer snap-start"
                onClick={() => navigate(`/product/${prod.id}`)}
              >
                <div className="bg-gray-100 rounded-md overflow-hidden aspect-square mb-2">
                  <img src={prod.imageUrl} alt={prod.name} className="w-full h-full object-cover mix-blend-multiply" />
                </div>
                <div className="text-xs text-gray-500 font-semibold mb-1">{prod.brand}</div>
                <div className="text-sm font-medium line-clamp-2 leading-snug mb-1">{prod.name}</div>
                <div className="flex items-center space-x-1">
                  {prod.isSale && (
                    <span className="text-red-500 font-bold text-sm">{prod.discountRate}%</span>
                  )}
                  <span className="font-bold text-sm">{prod.currentPrice.toLocaleString()}원</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
```

- [ ] **Step 2: Build the frontend**
Run: `cd frontend && npm run build`
Expected: Successfully compiled

- [ ] **Step 3: Commit**
```bash
git add frontend/src/pages/ProductDetail.tsx
git commit -m "feat(frontend): display similar products section on ProductDetail page"
```
