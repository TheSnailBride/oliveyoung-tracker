import type { Product } from '../api/products';

import ProductCard from './ProductCard';

interface ProductRailProps {
  title: string;
  description: string;
  products: Product[];
  showOriginalPrice?: boolean;
}

function ProductRail({
  title,
  description,
  products,
  showOriginalPrice = false,
}: ProductRailProps) {
  return (
    <section className="home-section">
      <div className="section-header">
        <h2 className="section-title">{title}</h2>
        <span className="section-desc">{description}</span>
      </div>
      <div className="scroll-row">
        {products.map(product => (
          <ProductCard
            key={product.id}
            product={product}
            variant="rail"
            showOriginalPrice={showOriginalPrice}
          />
        ))}
      </div>
    </section>
  );
}

export default ProductRail;
