package com.finsmart.repository;

import com.finsmart.model.Budget;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends MongoRepository<Budget, String> {

    List<Budget> findByUserId(String userId);

    Optional<Budget> findByUserIdAndCategoryAndPeriod(String userId, String category, String period);

    boolean existsByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
