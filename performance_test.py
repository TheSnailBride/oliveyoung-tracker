import argparse
import time
from dataclasses import dataclass
from typing import Callable


CATEGORY_NAME = "스킨/토너"
CATEGORY_ID = "100000100010013"
DEFAULT_PAGES = 4
DEFAULT_ROWS_PER_PAGE = 100
DEFAULT_WAIT_SECONDS = 1.5
DEFAULT_SCROLL_PAUSE_SECONDS = 0.8
DEFAULT_MAX_SCROLLS = 60
DEFAULT_STABLE_SCROLL_LIMIT = 5
SCRAPLING_CATEGORY_URL = "https://m.oliveyoung.co.kr/m/display/getMCategoryList.do"
SELENIUM_CATEGORY_URL = "https://www.oliveyoung.co.kr/store/display/getMCategoryList.do"
SELENIUM_CATEGORY_FETCH_HEADERS = {
    "Accept": "text/html, */*; q=0.01",
    "X-Requested-With": "XMLHttpRequest",
}
SELENIUM_PRODUCT_READY_SELECTOR = (
    "ul.cate_prd_list li, "
    ".prd_info, "
    "a[href*='goodsNo='], "
    "[data-ref-goodsno]"
)
MOBILE_USER_AGENT = (
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
    "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 "
    "Mobile/15E148 Safari/604.1"
)


@dataclass(frozen=True)
class PageResult:
    collected: int
    elapsed: float


def validate_pages(value):
    pages = int(value)
    if pages <= 0:
        raise ValueError("pages must be greater than 0")
    return pages


def build_category_url(page_idx, rows_per_page=DEFAULT_ROWS_PER_PAGE):
    return (
        f"{SCRAPLING_CATEGORY_URL}?dispCatNo={CATEGORY_ID}"
        f"&pageIdx={page_idx}"
        f"&rowsPerPage={rows_per_page}"
    )


def build_selenium_category_url(page_idx, rows_per_page=DEFAULT_ROWS_PER_PAGE):
    return (
        f"{SELENIUM_CATEGORY_URL}?dispCatNo={CATEGORY_ID}"
        f"&fltDispCatNo="
        f"&prdSort=01"
        f"&pageIdx={page_idx}"
        f"&rowsPerPage={rows_per_page}"
        f"&searchTypeSort=btn_thumb"
        f"&plusButtonFlag=N"
        f"&isLoginCnt=0"
        f"&aShowCnt=0"
        f"&bShowCnt=0"
        f"&cShowCnt=0"
        f"&trackingCd=Cat{CATEGORY_ID}_Small"
    )


def format_start_log(method, category, pages):
    return f"[CRAWL START] method={method}\ncategory={category}, pages={pages}"


def format_page_log(page_idx, collected, metric_name, elapsed):
    return f"[Page {page_idx}] collected={collected}, {metric_name}={elapsed:.1f}s"


def format_end_log(total, elapsed):
    return f"[CRAWL END] total={total}\nelapsed={elapsed:.1f}s"


def extract_id(url):
    if not url:
        return ""
    if "goodsNo=" in url:
        return url.split("goodsNo=", 1)[1].split("&", 1)[0]
    return url


def parse_price(price_text):
    if not price_text:
        return None
    cleaned = "".join(filter(str.isdigit, price_text))
    return int(cleaned) if cleaned else None


def first_text(element, by, selectors):
    for selector in selectors:
        matches = element.find_elements(by.CSS_SELECTOR, selector)
        if matches and matches[0].text.strip():
            return matches[0].text.strip()
    return ""


def first_attribute(element, by, selectors, attribute):
    for selector in selectors:
        matches = element.find_elements(by.CSS_SELECTOR, selector)
        if matches:
            value = matches[0].get_attribute(attribute)
            if value:
                return value.strip()
    return ""


def has_elements(element, by, selector):
    return bool(element.find_elements(by.CSS_SELECTOR, selector))


def parse_selenium_product_items(items, by, cat_name):
    parsed = []
    for item in items:
        try:
            name = first_text(item, by, [".tx_name", ".prd_name"])
            if not name:
                name = first_attribute(item, by, [".btn_zzim"], "data-ref-goodsnm")
            if not name:
                continue

            brand = first_text(item, by, [".tx_brand"])
            raw_url = first_attribute(item, by, ["a.prd_thumb", "a"], "href")
            olive_id = extract_id(raw_url) or item.get_attribute("data-ref-goodsno")
            if not olive_id:
                continue

            product_url = raw_url
            if product_url and not product_url.startswith("http"):
                product_url = "https://www.oliveyoung.co.kr" + product_url

            image_url = first_attribute(item, by, ["img"], "src")
            if not image_url or "blank" in image_url or "data:image" in image_url:
                image_url = first_attribute(item, by, ["img"], "data-original")

            price_text = first_text(item, by, [".prd_price .tx_cur .tx_num", ".tx_cur .tx_num"])
            current_price = parse_price(price_text)

            org_price_text = first_text(item, by, [".prd_price .tx_org .tx_num"])
            original_price = parse_price(org_price_text) if org_price_text else current_price

            parsed.append(
                {
                    "oliveYoungId": olive_id,
                    "name": name,
                    "brand": brand,
                    "category": cat_name,
                    "imageUrl": image_url,
                    "productUrl": product_url,
                    "currentPrice": current_price,
                    "originalPrice": original_price,
                    "discountRate": int((1 - current_price / original_price) * 100)
                    if original_price and current_price and original_price > current_price
                    else 0,
                    "isSale": original_price is not None and current_price is not None and original_price > current_price,
                    "isSoldOut": has_elements(item, by, ".soldout"),
                }
            )
        except Exception:
            pass
    return parsed


def should_continue_scrolling(collected, target_count, stable_scrolls, stable_limit, scroll_count, max_scrolls):
    return collected < target_count and stable_scrolls < stable_limit and scroll_count < max_scrolls


def extract_selenium_products_with_javascript(driver, cat_name):
    script = """
const category = arguments[0];
const seen = new Set();
const products = [];

function digits(text) {
  const value = String(text || '').replace(/[^0-9]/g, '');
  return value ? Number(value) : null;
}

function textOf(root, selectors) {
  for (const selector of selectors) {
    const element = root.querySelector(selector);
    const text = element && element.textContent && element.textContent.trim();
    if (text) return text;
  }
  return '';
}

function attrOf(root, selectors, attrs) {
  for (const selector of selectors) {
    const element = root.querySelector(selector);
    if (!element) continue;
    for (const attr of attrs) {
      const value = element.getAttribute(attr);
      if (value) return value.trim();
    }
    if (attrs.includes('currentSrc') && element.currentSrc) return element.currentSrc;
  }
  return '';
}

function goodsNoFrom(value) {
  if (!value) return '';
  const match = String(value).match(/[?&]goodsNo=([^&#]+)/);
  return match ? decodeURIComponent(match[1]) : '';
}

function productCardFrom(element) {
  return (
    element.closest('li') ||
    element.closest('[class*="product"]') ||
    element.closest('[class*="goods"]') ||
    element.closest('[class*="prd"]') ||
    element.parentElement ||
    element
  );
}

const nodes = Array.from(document.querySelectorAll(
  "a[href*='goodsNo='], [data-ref-goodsno], [data-ref-goods-no], button[data-ref-goodsno]"
));

for (const node of nodes) {
  const href = node.getAttribute('href') || '';
  const id =
    goodsNoFrom(href) ||
    node.getAttribute('data-ref-goodsno') ||
    node.getAttribute('data-ref-goods-no') ||
    '';
  if (!id || seen.has(id)) continue;

  const card = productCardFrom(node);
  const name =
    textOf(card, ['.tx_name', '.prd_name', '[class*="goodsNm"]', '[class*="goods-name"]', '[class*="name"]']) ||
    node.getAttribute('data-ref-goodsnm') ||
    node.getAttribute('aria-label') ||
    '';
  if (!name) continue;

  const brand = textOf(card, ['.tx_brand', '[class*="brand"]']);
  const imageUrl = attrOf(card, ['img'], ['data-original', 'data-src', 'src', 'currentSrc']);
  const currentPrice = digits(textOf(card, ['.prd_price .tx_cur .tx_num', '.tx_cur .tx_num', '[class*="price"]']));
  const originalPrice = digits(textOf(card, ['.prd_price .tx_org .tx_num', '.tx_org .tx_num', '[class*="origin"]'])) || currentPrice;
  const productUrl = href && href.startsWith('http') ? href : href ? 'https://www.oliveyoung.co.kr' + href : '';
  const isSoldOut = Boolean(card.querySelector('.soldout, [class*="soldout"], [class*="sold-out"]')) || card.textContent.includes('품절');

  seen.add(id);
  products.push({
    oliveYoungId: id,
    name,
    brand,
    category,
    imageUrl,
    productUrl,
    currentPrice,
    originalPrice,
    discountRate: originalPrice && currentPrice && originalPrice > currentPrice
      ? Math.floor((1 - currentPrice / originalPrice) * 100)
      : 0,
    isSale: Boolean(originalPrice && currentPrice && originalPrice > currentPrice),
    isSoldOut,
  });
}

return products;
"""
    return driver.execute_script(script, cat_name)


def extract_selenium_products_from_html(driver, html, cat_name):
    script = """
const html = arguments[0];
const category = arguments[1];
const doc = new DOMParser().parseFromString(html, 'text/html');
const seen = new Set();
const products = [];

function digits(text) {
  const value = String(text || '').replace(/[^0-9]/g, '');
  return value ? Number(value) : null;
}

function textOf(root, selectors) {
  for (const selector of selectors) {
    const element = root.querySelector(selector);
    const text = element && element.textContent && element.textContent.trim();
    if (text) return text;
  }
  return '';
}

function attrOf(root, selectors, attrs) {
  for (const selector of selectors) {
    const element = root.querySelector(selector);
    if (!element) continue;
    for (const attr of attrs) {
      const value = element.getAttribute(attr);
      if (value) return value.trim();
    }
    if (attrs.includes('currentSrc') && element.currentSrc) return element.currentSrc;
  }
  return '';
}

function goodsNoFrom(value) {
  if (!value) return '';
  const match = String(value).match(/[?&]goodsNo=([^&#]+)/);
  return match ? decodeURIComponent(match[1]) : '';
}

const rawNodes = Array.from(doc.querySelectorAll(
  "ul.cate_prd_list li, .cate_prd_list li, [data-ref-goodsno], a[href*='goodsNo=']"
));
const cards = rawNodes.map(node =>
  node.matches && node.matches("a[href*='goodsNo=']")
    ? (node.closest('li') || node)
    : node
);

for (const card of cards) {
  const link = card.matches && card.matches("a[href*='goodsNo=']")
    ? card
    : card.querySelector("a.prd_thumb, a[href*='goodsNo='], a");
  const href = link ? link.getAttribute('href') || '' : '';
  const id =
    goodsNoFrom(href) ||
    card.getAttribute('data-ref-goodsno') ||
    attrOf(card, ['.btn_zzim', '[data-ref-goodsno]'], ['data-ref-goodsno']) ||
    '';
  if (!id || seen.has(id)) continue;

  const name =
    textOf(card, ['.tx_name', '.prd_name', '[class*="goodsNm"]', '[class*="goods-name"]', '[class*="name"]']) ||
    attrOf(card, ['.btn_zzim'], ['data-ref-goodsnm']) ||
    '';
  if (!name) continue;

  const brand = textOf(card, ['.tx_brand', '[class*="brand"]']);
  const imageUrl = attrOf(card, ['img'], ['data-original', 'data-src', 'src', 'currentSrc']);
  const currentPrice = digits(textOf(card, ['.prd_price .tx_cur .tx_num', '.tx_cur .tx_num', '[class*="price"]']));
  const originalPrice = digits(textOf(card, ['.prd_price .tx_org .tx_num', '.tx_org .tx_num', '[class*="origin"]'])) || currentPrice;
  const productUrl = href && href.startsWith('http') ? href : href ? 'https://www.oliveyoung.co.kr' + href : '';
  const isSoldOut = Boolean(card.querySelector('.soldout, [class*="soldout"], [class*="sold-out"]')) || card.textContent.includes('품절');

  seen.add(id);
  products.push({
    oliveYoungId: id,
    name,
    brand,
    category,
    imageUrl,
    productUrl,
    currentPrice,
    originalPrice,
    discountRate: originalPrice && currentPrice && originalPrice > currentPrice
      ? Math.floor((1 - currentPrice / originalPrice) * 100)
      : 0,
    isSale: Boolean(originalPrice && currentPrice && originalPrice > currentPrice),
    isSoldOut,
  });
}

return products;
"""
    return driver.execute_script(script, html, cat_name)


def fetch_selenium_category_fragment(driver, url):
    script = """
const url = arguments[0];
const headers = arguments[1];
const done = arguments[arguments.length - 1];

fetch(url, {
  method: 'GET',
  credentials: 'include',
  headers,
})
  .then(async response => done({
    ok: response.ok,
    status: response.status,
    url: response.url,
    text: await response.text(),
  }))
  .catch(error => done({
    ok: false,
    status: 0,
    url,
    text: '',
    error: String(error),
  }));
"""
    return driver.execute_async_script(script, url, SELENIUM_CATEGORY_FETCH_HEADERS)


def fetch_selenium_category_products(driver, url, cat_name):
    fragment = fetch_selenium_category_fragment(driver, url)
    html = fragment.get("text", "")
    products = extract_selenium_products_from_html(driver, html, cat_name)
    return products, fragment


def collect_selenium_products_after_scroll(driver, args, cat_name):
    products = extract_selenium_products_with_javascript(driver, cat_name)
    previous_count = len(products)
    stable_scrolls = 0

    for scroll_count in range(args.max_scrolls):
        if not should_continue_scrolling(
            len(products),
            args.rows_per_page,
            stable_scrolls,
            args.stable_scroll_limit,
            scroll_count,
            args.max_scrolls,
        ):
            break

        driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
        time.sleep(args.scroll_pause_seconds)
        products = extract_selenium_products_with_javascript(driver, cat_name)

        current_count = len(products)
        if current_count <= previous_count:
            stable_scrolls += 1
        else:
            stable_scrolls = 0
        previous_count = current_count

    return products


def wait_for_selenium_product_items(driver, by, timeout):
    from selenium.common.exceptions import TimeoutException
    from selenium.webdriver.support.ui import WebDriverWait

    try:
        WebDriverWait(driver, timeout).until(
            lambda current_driver: len(
                current_driver.find_elements(by.CSS_SELECTOR, SELENIUM_PRODUCT_READY_SELECTOR)
            )
            > 0
        )
    except TimeoutException:
        pass
    legacy_items = driver.find_elements(by.CSS_SELECTOR, "ul.cate_prd_list li")
    if legacy_items:
        return legacy_items
    product_nodes = driver.find_elements(by.CSS_SELECTOR, "a[href*='goodsNo='], [data-ref-goodsno]")
    return product_nodes


def build_selenium_empty_diagnostic(driver, by):
    selectors = [
        "ul.cate_prd_list li",
        ".cate_prd_list li",
        "a[href*='goodsNo=']",
        "[data-ref-goodsno]",
        ".prd_name",
        ".tx_name",
        ".btn_zzim",
        "li",
    ]
    selector_counts = {
        selector: len(driver.find_elements(by.CSS_SELECTOR, selector))
        for selector in selectors
    }
    ready_state = driver.execute_script("return document.readyState")
    body_text = driver.find_element(by.TAG_NAME, "body").text[:180].replace("\n", " ")
    return (
        f"url={driver.current_url} title={driver.title!r} "
        f"ready={ready_state} selectors={selector_counts} body={body_text!r}"
    )


def is_selenium_blocked_page(driver, by):
    try:
        body_text = driver.find_element(by.TAG_NAME, "body").text
    except Exception:
        body_text = ""
    title = getattr(driver, "title", "") or ""
    markers = (
        "잠시만 기다려",
        "잠시만 기다리십시오",
        "접속 정보를 확인",
        "RAY_ID",
        "Just a moment",
        "Access Denied",
        "Forbidden",
    )
    return any(marker in title or marker in body_text for marker in markers)


def run_benchmark(
    method: str,
    pages: int,
    metric_name: str,
    fetch_page: Callable[[int], PageResult],
):
    print(format_start_log(method, CATEGORY_NAME, pages))
    started_at = time.perf_counter()
    total = 0

    for page_idx in range(1, pages + 1):
        result = fetch_page(page_idx)
        total += result.collected
        print(format_page_log(page_idx, result.collected, metric_name, result.elapsed))

    elapsed = time.perf_counter() - started_at
    print(format_end_log(total, elapsed))
    return total, elapsed


def run_selenium_benchmark(args):
    from selenium import webdriver
    from selenium.webdriver.chrome.options import Options
    from selenium.webdriver.common.by import By
    from selenium.webdriver.support.ui import WebDriverWait

    options = Options()
    if args.headless:
        options.add_argument("--headless=new")
    options.add_argument("--window-size=1440,1200")
    options.add_argument("--disable-gpu")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-blink-features=AutomationControlled")
    options.add_experimental_option("excludeSwitches", ["enable-automation"])
    options.add_experimental_option("useAutomationExtension", False)

    driver = webdriver.Chrome(options=options)
    driver.set_page_load_timeout(args.page_timeout)

    def fetch_page(page_idx):
        url = build_selenium_category_url(page_idx, args.rows_per_page)
        started_at = time.perf_counter()
        driver.get(url)
        WebDriverWait(driver, args.page_timeout).until(
            lambda current_driver: current_driver.execute_script("return document.readyState") == "complete"
        )
        time.sleep(args.wait_seconds)
        if is_selenium_blocked_page(driver, By):
            print(f"[Page {page_idx}] selenium-blocked {build_selenium_empty_diagnostic(driver, By)}")
            return PageResult(collected=0, elapsed=time.perf_counter() - started_at)
        wait_for_selenium_product_items(driver, By, args.page_timeout)
        items = driver.find_elements(By.CSS_SELECTOR, "ul.cate_prd_list li")
        products = parse_selenium_product_items(items, By, CATEGORY_NAME)
        if len(products) < args.rows_per_page:
            fetched_products, fragment = fetch_selenium_category_products(driver, url, CATEGORY_NAME)
            if len(fetched_products) > len(products):
                products = fetched_products
            elif len(products) <= 4:
                print(
                    f"[Page {page_idx}] selenium-fragment status={fragment.get('status')} "
                    f"url={fragment.get('url')} collected={len(fetched_products)}"
                )
        if len(products) < args.rows_per_page:
            scrolled_products = collect_selenium_products_after_scroll(driver, args, CATEGORY_NAME)
            if len(scrolled_products) > len(products):
                products = scrolled_products
        if not products:
            items = driver.find_elements(By.CSS_SELECTOR, "ul.cate_prd_list li")
            products = parse_selenium_product_items(items, By, CATEGORY_NAME)
        if not products:
            print(f"[Page {page_idx}] selenium-empty {build_selenium_empty_diagnostic(driver, By)}")
        return PageResult(collected=len(products), elapsed=time.perf_counter() - started_at)

    try:
        return run_benchmark("Selenium", args.pages, "render", fetch_page)
    finally:
        driver.quit()


def run_scrapling_benchmark(args):
    from scrapling import StealthyFetcher
    from scraper import parse_product_items

    def fetch_page(page_idx):
        url = build_category_url(page_idx, args.rows_per_page)
        started_at = time.perf_counter()
        page_data = StealthyFetcher.fetch(url, headless=True)
        fetch_elapsed = time.perf_counter() - started_at
        time.sleep(args.wait_seconds)
        items = page_data.css("ul.cate_prd_list li")
        products = parse_product_items(items, CATEGORY_NAME)
        return PageResult(collected=len(products), elapsed=fetch_elapsed)

    return run_benchmark("Scrapling", args.pages, "fetch", fetch_page)


def parse_args(argv=None):
    parser = argparse.ArgumentParser(
        description="Compare Selenium and Scrapling crawl speed for Olive Young skin/toner category."
    )
    parser.add_argument(
        "--method",
        choices=("both", "selenium", "scrapling"),
        default="both",
        help="Benchmark method to run. Default: both.",
    )
    parser.add_argument(
        "--pages",
        type=validate_pages,
        default=DEFAULT_PAGES,
        help=f"Number of category pages to crawl. Default: {DEFAULT_PAGES}.",
    )
    parser.add_argument(
        "--rows-per-page",
        type=int,
        default=DEFAULT_ROWS_PER_PAGE,
        help=f"Olive Young rowsPerPage parameter. Default: {DEFAULT_ROWS_PER_PAGE}.",
    )
    parser.add_argument(
        "--wait-seconds",
        type=float,
        default=DEFAULT_WAIT_SECONDS,
        help=f"Delay after page load/fetch before parsing. Default: {DEFAULT_WAIT_SECONDS}.",
    )
    parser.add_argument(
        "--page-timeout",
        type=int,
        default=30,
        help="Selenium page load timeout in seconds. Default: 30.",
    )
    parser.add_argument(
        "--scroll-pause-seconds",
        type=float,
        default=DEFAULT_SCROLL_PAUSE_SECONDS,
        help=f"Delay after each Selenium scroll. Default: {DEFAULT_SCROLL_PAUSE_SECONDS}.",
    )
    parser.add_argument(
        "--max-scrolls",
        type=int,
        default=DEFAULT_MAX_SCROLLS,
        help=f"Maximum Selenium scroll attempts per page. Default: {DEFAULT_MAX_SCROLLS}.",
    )
    parser.add_argument(
        "--stable-scroll-limit",
        type=int,
        default=DEFAULT_STABLE_SCROLL_LIMIT,
        help=f"Stop after this many scrolls without new products. Default: {DEFAULT_STABLE_SCROLL_LIMIT}.",
    )
    parser.add_argument(
        "--headless",
        action="store_true",
        dest="headless",
        help="Run Selenium in headless mode. This is more likely to be blocked by Olive Young.",
    )
    parser.add_argument(
        "--headed",
        action="store_false",
        dest="headless",
        help="Run Selenium with a visible browser window. This is the default.",
    )
    parser.set_defaults(headless=False)
    return parser.parse_args(argv)


def main():
    args = parse_args()
    if args.method in ("both", "selenium"):
        run_selenium_benchmark(args)
    if args.method == "both":
        print()
    if args.method in ("both", "scrapling"):
        run_scrapling_benchmark(args)


if __name__ == "__main__":
    main()
