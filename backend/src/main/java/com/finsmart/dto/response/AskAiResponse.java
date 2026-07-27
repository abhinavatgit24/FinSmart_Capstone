package com.finsmart.dto.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AskAiResponse {

    private String  answer;
    private boolean dataIncluded;
    private String  contextSummary;
}
