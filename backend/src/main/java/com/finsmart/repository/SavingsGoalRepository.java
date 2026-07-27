package com.finsmart.repository;

import com.finsmart.model.SavingsGoal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends MongoRepository<SavingsGoal, String> {

    List<SavingsGoal> findByUserIdOrderByCreatedAtDesc(String userId);

    boolean existsByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
