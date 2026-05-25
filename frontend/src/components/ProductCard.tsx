import { Link } from 'react-router-dom';

import type { Product } from '../api/products';

interface ProductCardProps {
  product: Product;
  variant?: 'grid' | 'rail';
  showOriginalPrice?: boolean;
}

function ProductCard({
  product,
  variant = 'grid',
  showOriginalPrice = false,
}: ProductCardProps) {
  if (variant === 'rail') {
    return (
      <Link to={`/product/${product.id}`} className="scroll-card">
        <div className="scroll-card-img">
          <img src={product.imageUrl} alt={product.name} loading="lazy" />
        </div>
        <div className="scroll-card-body">
          <p className="card-name">{product.name}</p>
          <p className="price-line">
            <span className="price-current">{product.currentPrice.toLocaleString()}원</span>
            {product.isSale && product.discountRate > 0 && <span className="discount-inline">{product.discountRate}%</span>}
          </p>
          {showOriginalPrice && (
            <p className="price-original" style={{ textDecoration: 'line-through', color: '#999', fontSize: '0.8rem' }}>
              {product.originalPrice.toLocaleString()}원
            </p>
          )}
        </div>
      </Link>
    );
  }

  return (
    <Link to={`/product/${product.id}`} className="product-card">
      <div className="card-image">
        <img src={product.imageUrl} alt={product.name} loading="lazy" />
      </div>
      <div className="card-body">
        <p className="card-brand">{product.brand}</p>
        <h3 className="card-name">{product.name}</h3>
        <p className="price-line">
          <span className="price-current">{product.currentPrice.toLocaleString()}원</span>
          {product.isSale && product.discountRate > 0 && <span className="discount-inline">{product.discountRate}%</span>}
        </p>
      </div>
    </Link>
  );
}

export default ProductCard;
