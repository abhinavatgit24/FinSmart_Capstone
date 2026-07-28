package com.finsmart.service;

import com.finsmart.dto.response.CsvImportResult;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Accepts a CSV file. Supported column names (case-insensitive, any order):
 *   date, amount, type (income/expense), description/narration/particulars, category
 *
 * Also handles bank-style exports where a single row has:
 *   debit, credit columns instead of amount + type.
 */
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final TransactionRepository transactionRepository;
    private final TransactionService    transactionService;   // for autoCategory()

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("d MMM yyyy")
    );

    public CsvImportResult importCsv(String userId, MultipartFile file) throws Exception {
        List<String>      errors       = new ArrayList<>();
        List<Transaction> saved        = new ArrayList<>();
        int totalRows = 0, skipped = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) throw new RuntimeException("CSV file is empty");

            // Parse headers
            String[] headers = splitCsv(headerLine);
            Map<String, Integer> colIdx = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colIdx.put(headers[i].trim().toLowerCase(), i);
            }

            // Detect column layout
            boolean hasSplit = colIdx.containsKey("debit") && colIdx.containsKey("credit");

            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                totalRows++;

                try {
                    String[] cells = splitCsv(line);

                    // ── Date ─────────────────────────────────────────────────
                    LocalDate date = parseDate(col(cells, colIdx, "date"));
                    if (date == null) {
                        errors.add("Row " + rowNum + ": unparseable date");
                        skipped++;
                        continue;
                    }

                    // ── Description ───────────────────────────────────────────
                    String description = firstNonEmpty(
                            col(cells, colIdx, "description"),
                            col(cells, colIdx, "narration"),
                            col(cells, colIdx, "particulars"),
                            col(cells, colIdx, "remarks")
                    );

                    // ── Amount + Type ─────────────────────────────────────────
                    double amount;
                    String type;

                    if (hasSplit) {
                        String debitStr  = col(cells, colIdx, "debit");
                        String creditStr = col(cells, colIdx, "credit");
                        double debit  = parseAmount(debitStr);
                        double credit = parseAmount(creditStr);

                        if (debit > 0) { amount = debit;  type = "expense"; }
                        else if (credit > 0) { amount = credit; type = "income"; }
                        else { skipped++; continue; } // both zero — skip
                    } else {
                        amount = parseAmount(col(cells, colIdx, "amount"));
                        if (amount <= 0) { skipped++; continue; }
                        String rawType = col(cells, colIdx, "type").toLowerCase();
                        type = rawType.contains("income") || rawType.contains("credit")
                                ? "income" : "expense";
                    }

                    // ── Category ──────────────────────────────────────────────
                    String category = col(cells, colIdx, "category");
                    if (category.isBlank()) {
                        category = transactionService.autoCategory(description);
                    }

                    Transaction txn = Transaction.builder()
                            .userId(userId)
                            .amount(amount)
                            .type(type)
                            .category(category)
                            .description(description)
                            .date(date)
                            .build();

                    saved.add(transactionRepository.save(txn));

                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                    skipped++;
                }
            }
        }

        return CsvImportResult.builder()
                .totalRows(totalRows)
                .imported(saved.size())
                .skipped(skipped)
                .errors(errors)
                .transactions(saved)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String col(String[] cells, Map<String, Integer> idx, String name) {
        Integer i = idx.get(name);
        if (i == null || i >= cells.length) return "";
        return cells[i].trim().replaceAll("^\"|\"$", ""); // strip surrounding quotes
    }

    private String firstNonEmpty(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }

    private double parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        // Strip currency symbols, commas, spaces
        String cleaned = raw.replaceAll("[₹$,\\s]", "").replaceAll("[^0-9.]", "");
        if (cleaned.isBlank()) return 0;
        try { return Double.parseDouble(cleaned); }
        catch (NumberFormatException e) { return 0; }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim().replaceAll("^\"|\"$", "");
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(cleaned, fmt); }
            catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    /** Naive CSV splitter — handles quoted fields containing commas */
    private String[] splitCsv(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { result.add(sb.toString()); sb.setLength(0); }
            else { sb.append(c); }
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }
}
