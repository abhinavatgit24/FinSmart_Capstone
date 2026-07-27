package com.finsmart.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "budgets")
@CompoundIndex(name = "user_cat_period_idx", def = "{'userId':1,'category':1,'period':1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    private String id;

    private String userId;

    private String category;        // matches Transaction.category

    private Double limitAmount;     // monthly or weekly cap

    private String period;          // "monthly" | "weekly"

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
