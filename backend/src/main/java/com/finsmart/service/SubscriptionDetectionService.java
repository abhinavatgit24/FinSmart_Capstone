package com.finsmart.service;

import com.finsmart.dto.response.SubscriptionResponse;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects recurring transactions by grouping expenses by (normalised merchant, amount-bucket)
 * and checking that they appear across 2+ distinct months.
 *
 * Amount bucket: amounts within ±2% of each other are considered the same charge
 * (handles rounding differences on subscription invoices).
 */
@Service
@RequiredArgsConstructor
public class SubscriptionDetectionService {

    private final TransactionRepository transactionRepository;

    // Tolerance: amounts within 2% of each other treated as same
    private static final double AMOUNT_TOLERANCE = 0.02;

    public List<SubscriptionResponse> detect(String userId) {
        // Look at last 6 months of expense transactions
        LocalDate from = LocalDate.now().minusMonths(6).withDayOfMonth(1);
        List<Transaction> txns = transactionRepository
                .findByUserIdAndDateBetweenOrderByDateDesc(userId, from, LocalDate.now())
                .stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .collect(Collectors.toList());

        if (txns.isEmpty()) return Collections.emptyList();

        // Group by normalised merchant key
        Map<String, List<Transaction>> byMerchant = new LinkedHashMap<>();
        for (Transaction t : txns) {
            String key = normaliseMerchant(t.getDescription());
            byMerchant.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        List<SubscriptionResponse> results = new ArrayList<>();

        for (Map.Entry<String, List<Transaction>> entry : byMerchant.entrySet()) {
            String merchant = entry.getKey();
            List<Transaction> group = entry.getValue();

            // Further cluster by amount bucket
            List<List<Transaction>> clusters = clusterByAmount(group);

            for (List<Transaction> cluster : clusters) {
                if (cluster.size() < 2) continue; // need at least 2 occurrences

                // Must span 2+ distinct calendar months
                long distinctMonths = cluster.stream()
                        .map(t -> t.getDate().getYear() * 12L + t.getDate().getMonthValue())
                        .distinct().count();
                if (distinctMonths < 2) continue;

                // Sort by date
                cluster.sort(Comparator.comparing(Transaction::getDate));
                List<LocalDate> dates = cluster.stream()
                        .map(Transaction::getDate).collect(Collectors.toList());

                double representativeAmount = cluster.stream()
                        .mapToDouble(Transaction::getAmount).average().orElse(0);

                String frequency = detectFrequency(dates);
                LocalDate lastCharged  = dates.get(dates.size() - 1);
                LocalDate nextExpected = predictNext(dates, frequency);
                double annualised = annualisedCost(representativeAmount, frequency);

                String category = cluster.stream()
                        .map(Transaction::getCategory)
                        .filter(Objects::nonNull)
                        .findFirst().orElse("Other");

                results.add(SubscriptionResponse.builder()
                        .merchant(capitalize(merchant))
                        .amount(round2(representativeAmount))
                        .frequency(frequency)
                        .lastCharged(lastCharged)
                        .nextExpected(nextExpected)
                        .occurrences(cluster.size())
                        .chargeDates(dates)
                        .category(category)
                        .annualisedCost(round2(annualised))
                        .build());
            }
        }

        // Sort by annualised cost descending
        results.sort(Comparator.comparingDouble(SubscriptionResponse::getAnnualisedCost).reversed());
        return results;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /**
     * Normalise merchant name: lowercase, strip common suffixes, keep first 3 meaningful words.
     */
    private String normaliseMerchant(String description) {
        if (description == null || description.isBlank()) return "unknown";
        String lower = description.toLowerCase()
                .replaceAll("(pvt\\.?|ltd\\.?|inc\\.?|llp|payment|auto-debit|autopay|sub|subscription)", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        // Take first 2 words (usually merchant name)
        String[] words = lower.split(" ");
        if (words.length >= 2) return words[0] + " " + words[1];
        return words[0];
    }

    private List<List<Transaction>> clusterByAmount(List<Transaction> txns) {
        List<List<Transaction>> clusters = new ArrayList<>();
        boolean[] assigned = new boolean[txns.size()];

        for (int i = 0; i < txns.size(); i++) {
            if (assigned[i]) continue;
            List<Transaction> cluster = new ArrayList<>();
            cluster.add(txns.get(i));
            assigned[i] = true;
            double ref = txns.get(i).getAmount();

            for (int j = i + 1; j < txns.size(); j++) {
                if (assigned[j]) continue;
                double other = txns.get(j).getAmount();
                if (Math.abs(other - ref) / ref <= AMOUNT_TOLERANCE) {
                    cluster.add(txns.get(j));
                    assigned[j] = true;
                }
            }
            clusters.add(cluster);
        }
        return clusters;
    }

    private String detectFrequency(List<LocalDate> dates) {
        if (dates.size() < 2) return "irregular";
        // Compute average gap in days between consecutive charges
        long totalGap = 0;
        for (int i = 1; i < dates.size(); i++) {
            totalGap += ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
        }
        double avgDays = (double) totalGap / (dates.size() - 1);

        if (avgDays >= 25 && avgDays <= 35) return "monthly";
        if (avgDays >= 5  && avgDays <= 9)  return "weekly";
        if (avgDays >= 13 && avgDays <= 16) return "fortnightly";
        if (avgDays >= 85 && avgDays <= 95) return "quarterly";
        if (avgDays >= 355)                 return "annual";
        return "irregular";
    }

    private LocalDate predictNext(List<LocalDate> dates, String frequency) {
        LocalDate last = dates.get(dates.size() - 1);
        return switch (frequency) {
            case "weekly"      -> last.plusWeeks(1);
            case "fortnightly" -> last.plusWeeks(2);
            case "monthly"     -> last.plusMonths(1);
            case "quarterly"   -> last.plusMonths(3);
            case "annual"      -> last.plusYears(1);
            default -> {
                // Use average gap
                if (dates.size() < 2) yield last.plusMonths(1);
                long gap = ChronoUnit.DAYS.between(dates.get(dates.size() - 2), last);
                yield last.plusDays(gap);
            }
        };
    }

    private double annualisedCost(double amount, String frequency) {
        return switch (frequency) {
            case "weekly"      -> amount * 52;
            case "fortnightly" -> amount * 26;
            case "monthly"     -> amount * 12;
            case "quarterly"   -> amount * 4;
            case "annual"      -> amount;
            default            -> amount * 12; // assume monthly
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
