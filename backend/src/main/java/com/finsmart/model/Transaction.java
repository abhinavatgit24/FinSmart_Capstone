package com.finsmart.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "transactions")
@CompoundIndexes({
        @CompoundIndex(name = "user_date_idx",     def = "{'userId': 1, 'date': -1}"),
        @CompoundIndex(name = "user_category_idx", def = "{'userId': 1, 'category': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    private String id;

    private String userId;

    private Double amount;

    private String type;       // "income" | "expense"

    private String category;

    private String description;

    private LocalDate date;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
