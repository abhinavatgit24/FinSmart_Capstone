package com.finsmart.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InsightResponse {
    private String  type;       // "warning" | "positive" | "info"
    private String  title;
    private String  body;       // full insight sentence
    private String  category;   // relevant category or null
    private Double  value;      // numeric value driving the insight
}
