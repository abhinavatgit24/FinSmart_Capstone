package com.finsmart.repository;

import com.finsmart.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByUserIdOrderByDateDesc(String userId);

    List<Transaction> findByUserIdAndTypeOrderByDateDesc(String userId, String type);

    List<Transaction> findByUserIdAndCategoryIgnoreCaseOrderByDateDesc(String userId, String category);

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate from, LocalDate to);

    List<Transaction> findTop5ByUserIdOrderByDateDesc(String userId);

    boolean existsByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);
}
