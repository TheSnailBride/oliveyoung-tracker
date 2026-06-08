import axios from 'axios';

import {
  ALL_CATEGORY_LABEL,
  getCategoriesParamForGroup,
  type CategoryGroup,
} from '../constants/categories.ts';

export const DEFAULT_PRICE_HISTORY_DAYS = 30;

export interface Product {
  id: number;
  name: string;
  brand: string;
  currentPrice: number;
  originalPrice: number;
  discountRate: number;
  imageUrl: string;
  isSale: boolean;
}

export interface ProductDetailData extends Product {
  productUrl: string;
  lowestPrice: number;
  highestPrice: number;
}

export interface PriceHistory {
  currentPrice: number;
  recordedAt: string;
}

export interface PageData {
  content: Product[];
  totalPages: number;
  number: number;
  totalElements: number;
}

export interface Stats {
  total: number;
  onSale: number;
  atLowest: number;
}

export interface ProductSearchFilters {
  page: number;
  size?: number;
  keyword?: string;
  category: string;
  selectedGroup?: CategoryGroup;
}

export interface ProductSearchParams {
  page: number;
  size: number;
  keyword: string | undefined;
  category: string | undefined;
  categories: string | undefined;
  sort: string;
}

export function buildProductSearchParams({
  page,
  size = 20,
  keyword,
  category,
  selectedGroup,
}: ProductSearchFilters): ProductSearchParams {
  const trimmedKeyword = keyword?.trim();
  const isAllCategory = category === ALL_CATEGORY_LABEL;

  return {
    page,
    size,
    keyword: trimmedKeyword || undefined,
    category: !isAllCategory ? category : undefined,
    categories: isAllCategory ? getCategoriesParamForGroup(selectedGroup) : undefined,
    sort: 'updatedAt,desc',
  };
}

export async function fetchProductStats(): Promise<Stats> {
  const response = await axios.get('/api/products/stats');
  return response.data.data;
}

export async function fetchAtLowestProducts(size = 10): Promise<Product[]> {
  const response = await axios.get('/api/products/at-lowest', { params: { size } });
  return response.data.data;
}

export async function fetchTopDiscountedProducts(size = 10): Promise<Product[]> {
  const response = await axios.get('/api/products/top-discounted', { params: { size } });
  return response.data.data.content;
}

export async function fetchPriceDroppedProducts(categories?: string, size = 9): Promise<Product[]> {
  const response = await axios.get('/api/products/price-drops', {
    params: {
      categories,
      size,
    },
  });
  return response.data.data;
}

export async function fetchProducts(filters: ProductSearchFilters): Promise<PageData> {
  const response = await axios.get('/api/products', {
    params: buildProductSearchParams(filters),
  });
  return response.data.data;
}

export async function fetchProductDetail(productId: string | number): Promise<ProductDetailData> {
  const response = await axios.get(`/api/products/${productId}`);
  return response.data.data;
}

export async function fetchPriceHistory(productId: string | number, days: number): Promise<PriceHistory[]> {
  const response = await axios.get(`/api/products/${productId}/prices`, {
    params: { days },
  });
  return response.data.data;
}

export async function fetchSimilarProducts(productId: string | number): Promise<Product[]> {
  const response = await axios.get(`/api/products/${productId}/similar`);
  return response.data.data;
}
