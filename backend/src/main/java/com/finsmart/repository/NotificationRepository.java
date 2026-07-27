package com.finsmart.repository;

import com.finsmart.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    long countByUserIdAndReadFalse(String userId);
    boolean existsByUserIdAndRefIdAndType(String userId, String refId, String type);
    List<Notification> findByUserIdAndReadFalse(String userId);
}
