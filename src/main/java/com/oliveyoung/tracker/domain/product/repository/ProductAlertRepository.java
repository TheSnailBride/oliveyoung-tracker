package com.oliveyoung.tracker.domain.product.repository;

import com.oliveyoung.tracker.domain.product.entity.Product;
import com.oliveyoung.tracker.domain.product.entity.ProductAlert;
import com.oliveyoung.tracker.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAlertRepository extends JpaRepository<ProductAlert, Long> {
    Optional<ProductAlert> findByUserAndProduct(User user, Product product);
    boolean existsByUserAndProduct(User user, Product product);
    void deleteByUserAndProduct(User user, Product product);
    List<ProductAlert> findByProduct(Product product);
    List<ProductAlert> findByUser(User user);
}
