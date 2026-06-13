package com.ecommerce.infrastructure.persistence;

import com.ecommerce.domain.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;

//@Repository
public interface CategoriaJpaRepository extends JpaRepository<Categoria, Long> {
}
