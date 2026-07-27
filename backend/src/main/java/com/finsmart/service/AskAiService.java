package com.finsmart.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsmart.dto.response.AskAiResponse;
import com.finsmart.dto.response.BudgetUtilisationResponse;
import com.finsmart.dto.response.FinancialHealthResponse;
import com.finsmart.dto.response.SavingsGoalResponse;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import com.finsmart.util.DateQueryHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AskAiService {

    private final TransactionRepository  transactionRepository;
    private final BudgetService          budgetService;
    private final SavingsGoalService     goalService;
    private final FinancialHealthService healthService;

    /**
     * Spring's auto-configured ObjectMapper already has JavaTimeModule registered.
     * Do NOT replace with new ObjectMapper() — that loses JavaTimeModule and crashes
     * on LocalDate fields in SavingsGoalResponse.
     */
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final RestTemplate restTemplate = new RestTemplate();

    public AskAiResponse ask(String userId, String question) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDate historyStart = currentMonth.minusMonths(5).atDay(1);

        // One compact six-month dataset supports current-month, last-month and
        // comparison questions without sending the model raw transaction details.
        List<Transaction> historyTxns = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, historyStart, today);
        List<Transaction> currentTxns = transactionsForMonth(historyTxns, currentMonth);
        List<Transaction> previousTxns = transactionsForMonth(historyTxns, previousMonth);

        List<BudgetUtilisationResponse> budgets = budgetService.getUtilisation(userId);
        List<SavingsGoalResponse>       goals   = goalService.getAllWithPrediction(userId);
        FinancialHealthResponse         health  = healthService.compute(userId);

        boolean hasData = !historyTxns.isEmpty();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("today", today.toString());
        context.put("currentMonth", monthlySummary(currentMonth, currentTxns));
        context.put("previousMonth", monthlySummary(previousMonth, previousTxns));
        context.put("lastSixMonths", monthlySummaries(historyTxns, currentMonth));
        context.put("budgets",               budgets);
        context.put("savingsGoals",          goals);
        Map<String, Object> healthSummary = new LinkedHashMap<>();
        healthSummary.put("score", health.getScore());
        healthSummary.put("band", health.getBand());
        context.put("financialHealthScore", healthSummary);

        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            log.error("Failed to serialise AI context for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to build AI context: " + e.getMessage());
        }

        String systemInstruction = """
                You are a financial assistant inside the FinSmart personal finance app.
                You will be given a JSON snapshot of the user's financial data,
                followed by their question.

                STRICT RULES:
                1. ONLY use numbers and facts present in the JSON data provided.
                   Never invent, estimate, or assume any figure not explicitly given.
                2. The data includes the current month, previous month and up to six
                   monthly summaries. If the requested period is outside that range,
                   say it is unavailable rather than guessing.
                3. If the data needed is missing or zero, say so honestly.
                4. Keep answers concise: 2-4 sentences, plain English, no markdown.
                5. You may give suggestions but only grounded in actual numbers.
                6. Do not discuss anything unrelated to the user's finances.
                7. ALWAYS use ₹ (Indian Rupee) for all monetary amounts. Never use $.
                """;

        String fullPrompt = systemInstruction
                + "\n\nFINANCIAL DATA (JSON):\n" + contextJson
                + "\n\nUSER QUESTION:\n" + question;

        String answer = callGemini(fullPrompt);

        return AskAiResponse.builder()
                .answer(answer)
                .dataIncluded(hasData)
                .contextSummary(hasData
                        ? "Based on your current, previous, and recent six-month financial data"
                        : "No transactions found in the last six months")
                .build();
    }

    private List<Transaction> transactionsForMonth(List<Transaction> transactions, YearMonth month) {
        return transactions.stream()
                .filter(t -> t.getDate() != null && YearMonth.from(t.getDate()).equals(month))
                .toList();
    }

    private Map<String, Object> monthlySummary(YearMonth month, List<Transaction> transactions) {
        double income = sumByType(transactions, "income");
        double expense = sumByType(transactions, "expense");
        Map<String, Double> byCategory = transactions.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "Other",
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("month", month.toString());
        summary.put("income", round2(income));
        summary.put("expenses", round2(expense));
        summary.put("savings", round2(income - expense));
        summary.put("expensesByCategory", byCategory);
        return summary;
    }

    private List<Map<String, Object>> monthlySummaries(List<Transaction> transactions, YearMonth currentMonth) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth month = currentMonth.minusMonths(offset);
            summaries.add(monthlySummary(month, transactionsForMonth(transactions, month)));
        }
        return summaries;
    }

    private String callGemini(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.3, "maxOutputTokens", 300)
        );

        String url = GEMINI_URL + "?key=" + geminiApiKey;

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return extractText(response.getBody());
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Gemini rate limit hit");
            return "I'm receiving too many requests — please try again in a minute.";
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return "I couldn't reach the AI service just now. Please try again shortly.";
        }
    }

    private String extractText(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            return root.path("candidates").get(0)
                       .path("content").path("parts").get(0)
                       .path("text")
                       .asText("I couldn't generate a response — please try rephrasing.");
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            return "I couldn't understand the AI response. Please try again.";
        }
    }

    private double sumByType(List<Transaction> txns, String type) {
        return txns.stream()
                .filter(t -> type.equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
