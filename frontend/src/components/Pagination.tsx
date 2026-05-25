import type { PageData } from '../api/products';

interface PaginationProps {
  page: PageData;
  onPageChange: (pageIndex: number) => void;
}

function Pagination({ page, onPageChange }: PaginationProps) {
  if (page.totalPages <= 1) {
    return null;
  }

  const start = Math.max(0, Math.min(page.totalPages - 5, page.number - 2));
  const pageIndexes = Array.from(
    { length: Math.min(5, page.totalPages) },
    (_, index) => start + index,
  ).filter(pageIndex => pageIndex >= 0 && pageIndex < page.totalPages);

  return (
    <div className="pagination">
      {pageIndexes.map(pageIndex => (
        <button
          key={pageIndex}
          className={page.number === pageIndex ? 'active' : ''}
          onClick={() => onPageChange(pageIndex)}
        >
          {pageIndex + 1}
        </button>
      ))}
    </div>
  );
}

export default Pagination;
