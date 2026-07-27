package com.finsmart.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "savings_goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsGoal {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String name;

    private Double targetAmount;

    private Double savedAmount;     // manually tracked current progress

    private LocalDate deadline;     // user-set target date

    @Builder.Default
    private String status = "active"; // "active" | "completed"

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
