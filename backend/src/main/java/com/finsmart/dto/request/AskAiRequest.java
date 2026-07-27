package com.finsmart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AskAiRequest {

    @NotBlank(message = "Question cannot be empty")
    @Size(max = 500, message = "Question must be under 500 characters")
    private String question;
}
