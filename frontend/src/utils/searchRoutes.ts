export function buildSearchResultsPath(keyword: string): string | null {
  const trimmedKeyword = keyword.trim();
  if (!trimmedKeyword) {
    return null;
  }

  const params = new URLSearchParams({
    keyword: trimmedKeyword,
    page: '0',
  });

  return `/search?${params.toString()}`;
}
