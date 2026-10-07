package com.farm.portal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProduceRepository extends JpaRepository<Produce, Long> {
    List<Produce> findByNameContainingIgnoreCaseOrFarmContainingIgnoreCase(String name, String farm);
    long countByStatus(Status status);
}
