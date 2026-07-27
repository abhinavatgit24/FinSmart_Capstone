package com.finsmart.controller;

import com.finsmart.dto.request.AskAiRequest;
import com.finsmart.dto.response.AskAiResponse;
import com.finsmart.service.AskAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AskAiController {

    private final AskAiService askAiService;

    /**
     * POST /api/ai/ask
     * Protected — JWT required (handled by SecurityConfig anyRequest().authenticated()).
     * Body: { "question": "How much have I spent on food this month?" }
     */
    @PostMapping("/ask")
    public ResponseEntity<AskAiResponse> ask(
            @AuthenticationPrincipal UserDetails ud,
            @Valid @RequestBody AskAiRequest req) {

        return ResponseEntity.ok(askAiService.ask(ud.getUsername(), req.getQuestion()));
    }
}
