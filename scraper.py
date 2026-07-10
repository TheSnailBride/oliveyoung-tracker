import json
import logging
import os
import time
import requests
from scrapling import StealthyFetcher

logging.getLogger('scrapling').setLevel(logging.WARNING)

# Backend API Endpoints
SERVER_PORT = os.getenv("SERVER_PORT", "8080").strip() or "8080"
BACKEND_INTERNAL_BASE_URL = os.getenv(
    "BACKEND_INTERNAL_BASE_URL",
    f"http://localhost:{SERVER_PORT}/api"
).rstrip("/")
BASE_API_URL = f"{BACKEND_INTERNAL_BASE_URL}/crawler"
IMPORT_API_URL = f"{BASE_API_URL}/import"
GET_PRODUCTS_API_URL = f"{BACKEND_INTERNAL_BASE_URL}/products/all-for-crawler"
CRAWLER_INTERNAL_TOKEN = os.getenv("CRAWLER_INTERNAL_TOKEN", "").strip()

# Olive Young URLs
BEST_URL = "https://m.oliveyoung.co.kr/m/main/getBestList.do"
PRODUCT_DETAIL_URL = "https://m.oliveyoung.co.kr/m/goods/getGoodsDetail.do?goodsNo="
CATEGORY_WAIT_SECONDS = 5.0
CATEGORY_RETRY_DELAYS = [30, 30]
CATEGORY_BLOCKED_MARKERS = ("Just a moment", "Access Denied", "Forbidden", "잠시만 기다려 주세요", "접속 정보를 확인")

# 사용자가 지정한 15자리 하위 카테고리 ID 목록
TARGET_CATEGORIES = {
    # --- 스킨케어 ---
    "스킨/토너": "100000100010013",
    "에센스/세럼/앰플": "100000100010014",
    "크림": "100000100010015",
    "로션": "100000100010016",
    "미스트/오일": "100000100010010",
    "스킨케어세트": "100000100010017",
    "스킨케어 디바이스": "100000100010018",

    # --- 마스크팩 ---
    "시트팩": "100000100090001",
    "패드": "100000100090004",
    "페이셜팩": "100000100090002",
    "코팩": "100000100090005",
    "패치": "100000100090006",

    # --- 클렌징 ---
    "클렌징폼/젤": "100000100100001",
    "오일/밤": "100000100100004",
    "워터/밀크": "100000100100005",
    "필링&스크럽": "100000100100007",
    "티슈/패드": "100000100100008",
    "립&아이리무버": "100000100100006",
    "클렌징 디바이스": "100000100100009",

    # --- 선케어 ---
    "선크림": "100000100110006",
    "선스틱": "100000100110003",
    "선쿠션": "100000100110004",
    "선스프레이/선패치": "100000100110005",
    "태닝/애프터선": "100000100110002",

    # --- 메이크업 ---
    "립메이크업": "100000100020006",
    "베이스메이크업": "100000100020001",
    "아이메이크업": "100000100020007",

    # --- 뷰티소품 ---
    "메이크업 툴": "100000100060001",
    "아이래쉬 툴": "100000100060007",
    "페이스 툴": "100000100060006",
    "헤어/바디 툴": "100000100060002",
    "데일리 툴": "100000100060005",

    # --- 더모 코스메틱 ---
    "더모_스킨케어": "100000100080013",
    "더모_바디케어": "100000100080004",
    "더모_클렌징": "100000100080006",
    "더모_선케어": "100000100080005",
    "더모_마스크팩": "100000100080011",

    # --- 네일 ---
    "일반네일": "100000100120007",
    "젤네일": "100000100120010",
    "네일팁/스티커": "100000100120008",
    "네일케어/리무버": "100000100120004",

    # --- 헤어케어 ---
    "샴푸/스케일러": "100000100040008",
    "트리트먼트/팩": "100000100040007",
    "두피에센스": "100000100040014",
    "헤어에센스": "100000100040013",
    "염모제/펌": "100000100040010",
    "헤어기기/브러시": "100000100040004",
    "스타일링": "100000100040011",

    # --- 바디케어 ---
    "샤워/입욕": "100000100030005",
    "바스로션/크림": "100000100030025",
    "바스오일/미스트": "100000100030022",
    "제모/왁싱": "100000100030019",
    "데오드란트": "100000100030012",
    "핸드케어": "100000100030016",
    "풋케어": "100000100030024",
    "유아동/임산부": "100000100030020",

    # --- 향수/디퓨저 ---
    "향수": "100000100050013",
    "미니/고체향수": "100000100050010",
    "홈프래그런스": "100000100050012",

    # --- 건강식품 ---
    "비타민": "100000200010015",
    "영양제": "100000200010025",
    "유산균": "100000200010024",
    "슬리밍/이너뷰티": "100000200010023",

    # --- 푸드 ---
    "식단관리/이너뷰티": "100000200020020",
    "과자/초콜릿/디저트": "100000200020023",
    "생수/음료/커피": "100000200020022",
    "간편식/요리": "100000200020024",
    "베이비푸드": "100000200020021"
}

def extract_id(url):
    if not url:
        return ""
    if "goodsNo=" in url:
        return url.split("goodsNo=")[1].split("&")[0]
    return url

def parse_price(price_str):
    if not price_str:
        return None
    cleaned = ''.join(filter(str.isdigit, price_str))
    return int(cleaned) if cleaned else None

def is_dermo_category(category_name):
    return bool(category_name) and category_name.startswith("더모_")

def should_replace_collected_product(existing_product, candidate_product):
    if not existing_product:
        return True

    existing_category = existing_product.get("category")
    candidate_category = candidate_product.get("category")
    return is_dermo_category(candidate_category) and not is_dermo_category(existing_category)

def merge_collected_product(collected_products, product):
    olive_id = product.get("oliveYoungId")
    if not olive_id:
        return

    existing_product = collected_products.get(olive_id)
    candidate_categories = normalize_categories(product)
    if should_replace_collected_product(existing_product, product):
        existing_categories = normalize_categories(existing_product)
        replacement = dict(product)
        replacement["categories"] = list(dict.fromkeys(existing_categories + candidate_categories))
        collected_products[olive_id] = replacement
        return

    existing_categories = normalize_categories(existing_product)
    existing_product["categories"] = list(dict.fromkeys(existing_categories + candidate_categories))


def normalize_categories(product):
    if not product:
        return []

    categories = product.get("categories") or []
    if isinstance(categories, str):
        categories = [categories]

    category = product.get("category")
    values = list(categories) + ([category] if category else [])
    return list(dict.fromkeys(
        value.strip() for value in values
        if isinstance(value, str) and value.strip()
    ))

def get_db_products():
    print("[Python Scraper] Fetching existing products from DB...")
    try:
        response = requests.get(GET_PRODUCTS_API_URL, headers=internal_headers(), timeout=10)
        if response.status_code == 200:
            data = response.json()
            products_list = data.get("data", [])
            # { "A000000000": "https://..." } 형태로 매핑
            return {p.get("oliveYoungId"): p.get("productUrl") for p in products_list if p.get("oliveYoungId")}
        else:
            print(f"Failed to fetch DB products. Status: {response.status_code}")
            return {}
    except Exception as e:
        print(f"Could not connect to backend to fetch products: {e}")
        return {}

def send_to_backend(products, category_name="Batch"):
    if not products:
        return

    # 50개 단위 배치 전송
    batch_size = 50
    headers = internal_headers({'Content-type': 'application/json'})
    for i in range(0, len(products), batch_size):
        batch = products[i:i+batch_size]
        print(f"[{category_name}] Sending batch of {len(batch)} products to Spring Boot Backend...")
        try:
            response = requests.post(IMPORT_API_URL, data=json.dumps(batch), headers=headers, timeout=15)
            if response.status_code == 200:
                print(f"Success! Backend responded: {response.json().get('message', 'OK')}")
            else:
                print(f"Failed to send data. Status code: {response.status_code}, Response: {response.text}")
        except Exception as e:
            print(f"Could not connect to backend at {IMPORT_API_URL}. Is Spring Boot running? Error: {e}")

def internal_headers(base_headers=None):
    headers = dict(base_headers or {})
    if CRAWLER_INTERNAL_TOKEN:
        headers["X-Crawler-Token"] = CRAWLER_INTERNAL_TOKEN
    return headers

def parse_product_items(items, cat_name):
    parsed = []
    for item in items:
        try:
            name = ""
            name_el = item.css(".tx_name") or item.css(".prd_name")
            if name_el:
                name = name_el[0].text.strip()

            if not name:
                zzim_btn = item.css(".btn_zzim")
                if zzim_btn:
                    name = zzim_btn[0].attrib.get("data-ref-goodsnm", "").strip()

            if not name: continue

            brand = ""
            brand_el = item.css(".tx_brand")
            if brand_el:
                brand = brand_el[0].text.strip()

            link_el = item.css("a.prd_thumb") or item.css("a")
            if not link_el: continue

            raw_url = link_el[0].attrib.get("href", "")
            olive_id = extract_id(raw_url) or item.attrib.get("data-ref-goodsno", "")
            if not olive_id: continue

            product_url = raw_url
            if product_url and not product_url.startswith("http"):
                product_url = "https://www.oliveyoung.co.kr" + product_url

            # Images
            img_el = item.css("img")
            image_url = ""
            if img_el:
                image_url = img_el[0].attrib.get("src", "")
                if not image_url or "blank" in image_url or "data:image" in image_url:
                    image_url = img_el[0].attrib.get("data-original", "")

            # Price
            price_el = item.css(".prd_price .tx_cur .tx_num") or item.css(".tx_cur .tx_num")
            price_text = price_el[0].text if price_el else ""
            current_price = parse_price(price_text)

            # Original Price
            org_price_el = item.css(".prd_price .tx_org .tx_num")
            org_price = parse_price(org_price_el[0].text) if org_price_el else current_price

            # Sold out
            soldout_el = item.css(".soldout")
            is_sold_out = True if soldout_el else False

            parsed.append({
                "oliveYoungId": olive_id,
                "name": name,
                "brand": brand,
                "category": cat_name,
                "categories": [cat_name],
                "imageUrl": image_url,
                "productUrl": product_url,
                "currentPrice": current_price,
                "originalPrice": org_price,
                "discountRate": int((1 - current_price/org_price) * 100) if org_price and org_price > current_price else 0,
                "isSale": org_price is not None and org_price > current_price,
                "isSoldOut": is_sold_out
            })
        except Exception as e:
            pass
    return parsed

def is_blocked_or_error_page(page_data):
    status = getattr(page_data, "status", None)
    if status in (403, 429, 503):
        return True

    body = str(getattr(page_data, "body", ""))[:2000]
    return any(marker in body for marker in CATEGORY_BLOCKED_MARKERS)

def scrape_category_page(url, cat_name, retry_empty=True):
    last_reason = ""
    for attempt in range(1, len(CATEGORY_RETRY_DELAYS) + 2):
        fetch_start = time.time()
        try:
            page_data = StealthyFetcher.fetch(url, headless=True)
            fetch_elapsed = time.time() - fetch_start
        except Exception as e:
            fetch_elapsed = time.time() - fetch_start
            last_reason = f"exception={type(e).__name__}"
            if attempt <= len(CATEGORY_RETRY_DELAYS):
                delay = CATEGORY_RETRY_DELAYS[attempt - 1]
                print(f"    Attempt {attempt} failed ({last_reason}, fetch={fetch_elapsed:.2f}s). Retrying same page in {delay}s...")
                time.sleep(delay)
                continue
            return [], fetch_elapsed, 0, last_reason

        time.sleep(CATEGORY_WAIT_SECONDS)

        parse_start = time.time()
        items = page_data.css("ul.cate_prd_list li")
        products = parse_product_items(items, cat_name)
        parse_elapsed = time.time() - parse_start

        if products:
            return products, fetch_elapsed, parse_elapsed, ""

        status = getattr(page_data, "status", None)
        blocked = is_blocked_or_error_page(page_data)
        if status is not None and status != 200:
            last_reason = f"status={status}"
        elif not items:
            last_reason = "empty-items"
        else:
            last_reason = "empty-products"

        should_retry = blocked or (retry_empty and last_reason in ("empty-items", "empty-products"))
        if should_retry and attempt <= len(CATEGORY_RETRY_DELAYS):
            delay = CATEGORY_RETRY_DELAYS[attempt - 1]
            print(f"    Attempt {attempt} blocked or empty ({last_reason}). Retrying same page in {delay}s...")
            time.sleep(delay)
            continue

        return [], fetch_elapsed, parse_elapsed, last_reason

    return [], 0, 0, last_reason

def scrape_oliveyoung():
    print("[Python Scraper] Starting full update process (Category Pagination Strategy)...")
    start_time_total = time.time()

    # 중복 방지를 위한 데이터 저장소 {olive_id: product_data}
    collected_products = {}

    # 1. 카테고리 페이지 크롤링
    category_start_time = time.time()
    for cat_name, cat_id in TARGET_CATEGORIES.items():
        print(f"\nCrawling Category Pages: [{cat_name}]")
        for page_idx in range(1, 20): # 최대 15페이지까지
            url = f"https://m.oliveyoung.co.kr/m/display/getMCategoryList.do?dispCatNo={cat_id}&pageIdx={page_idx}&rowsPerPage=100"
            try:
                products, fetch_elapsed, parse_elapsed, failure_reason = scrape_category_page(url, cat_name)
                if not products:
                    if failure_reason.startswith("status="):
                        print(f"  [Page {page_idx}] 상품 목록 조회 실패({failure_reason}). 다음 카테고리로 넘어갑니다.")
                    else:
                        print(f"  [Page {page_idx}] 상품이 더 이상 없습니다. 다음 카테고리로 넘어갑니다.")
                    break

                for p in products:
                    merge_collected_product(collected_products, p)

                print(f"  [Page {page_idx}] Added {len(products)} products. fetch={fetch_elapsed:.2f}s parse={parse_elapsed:.2f}s")

                # 100개를 채우지 못했다면 그 페이지가 마지막이라는 뜻 (대화 10.txt 요구사항 반영)
                if len(products) < 100:
                    print(f"  [Page {page_idx}] 마지막 페이지 도달 추정({len(products)}개). 다음 카테고리로 넘어갑니다.")
                    break

            except Exception as e:
                print(f"  Error crawling [{cat_name}] Page {page_idx}: {e}")
                break

    category_end_time = time.time()
    print(f"\n[Time Metrics] Category Pages Crawl completed in {category_end_time - category_start_time:.2f} seconds.")

    # 2. 결과 통합 및 백엔드 전송
    transfer_start_time = time.time()
    all_results = list(collected_products.values())
    print(f"\n[Finalizing] Total unique products to update: {len(all_results)}")

    send_to_backend(all_results, category_name="Final Category Update")

    transfer_end_time = time.time()
    print(f"[Time Metrics] Data Transfer completed in {transfer_end_time - transfer_start_time:.2f} seconds.")

    end_time_total = time.time()
    print(f"\n[Scraping Completed] Total Elapsed Time: {end_time_total - start_time_total:.2f} seconds.")

if __name__ == "__main__":
    scrape_oliveyoung()
