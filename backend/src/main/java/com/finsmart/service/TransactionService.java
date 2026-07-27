package com.finsmart.service;

import com.finsmart.dto.request.TransactionRequest;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    // ── Keyword → Category map (50+ keywords, order matters) ─────────────────
    private static final LinkedHashMap<String, String> KEYWORD_MAP = new LinkedHashMap<>();
    static {
        // Food
        KEYWORD_MAP.put("swiggy",      "Food");
        KEYWORD_MAP.put("zomato",      "Food");
        KEYWORD_MAP.put("bigbasket",   "Food");
        KEYWORD_MAP.put("dunzo",       "Food");
        KEYWORD_MAP.put("blinkit",     "Food");
        KEYWORD_MAP.put("instamart",   "Food");
        KEYWORD_MAP.put("restaurant",  "Food");
        KEYWORD_MAP.put("cafe",        "Food");
        KEYWORD_MAP.put("grocery",     "Food");
        KEYWORD_MAP.put("groceries",   "Food");
        KEYWORD_MAP.put("supermarket", "Food");
        KEYWORD_MAP.put("bakery",      "Food");
        KEYWORD_MAP.put("dairy",       "Food");

        // Travel
        KEYWORD_MAP.put("uber",        "Travel");
        KEYWORD_MAP.put("ola",         "Travel");
        KEYWORD_MAP.put("rapido",      "Travel");
        KEYWORD_MAP.put("irctc",       "Travel");
        KEYWORD_MAP.put("makemytrip",  "Travel");
        KEYWORD_MAP.put("goibibo",     "Travel");
        KEYWORD_MAP.put("cleartrip",   "Travel");
        KEYWORD_MAP.put("metro",       "Travel");
        KEYWORD_MAP.put("fuel",        "Travel");
        KEYWORD_MAP.put("petrol",      "Travel");
        KEYWORD_MAP.put("diesel",      "Travel");
        KEYWORD_MAP.put("flight",      "Travel");
        KEYWORD_MAP.put("train",       "Travel");
        KEYWORD_MAP.put("bus",         "Travel");
        KEYWORD_MAP.put("parking",     "Travel");
        KEYWORD_MAP.put("toll",        "Travel");

        // Bills
        KEYWORD_MAP.put("rent",        "Bills");
        KEYWORD_MAP.put("electricity", "Bills");
        KEYWORD_MAP.put("water bill",  "Bills");
        KEYWORD_MAP.put("internet",    "Bills");
        KEYWORD_MAP.put("broadband",   "Bills");
        KEYWORD_MAP.put("wifi",        "Bills");
        KEYWORD_MAP.put("jio",         "Bills");
        KEYWORD_MAP.put("airtel",      "Bills");
        KEYWORD_MAP.put("bsnl",        "Bills");
        KEYWORD_MAP.put("vi ",         "Bills");
        KEYWORD_MAP.put("mobile bill", "Bills");
        KEYWORD_MAP.put("recharge",    "Bills");
        KEYWORD_MAP.put("emi",         "Bills");
        KEYWORD_MAP.put("insurance",   "Bills");
        KEYWORD_MAP.put("loan",        "Bills");

        // Entertainment
        KEYWORD_MAP.put("netflix",     "Entertainment");
        KEYWORD_MAP.put("spotify",     "Entertainment");
        KEYWORD_MAP.put("prime video", "Entertainment");
        KEYWORD_MAP.put("hotstar",     "Entertainment");
        KEYWORD_MAP.put("youtube premium", "Entertainment");
        KEYWORD_MAP.put("disney",      "Entertainment");
        KEYWORD_MAP.put("sonyliv",     "Entertainment");
        KEYWORD_MAP.put("zee5",        "Entertainment");
        KEYWORD_MAP.put("movie",       "Entertainment");
        KEYWORD_MAP.put("cinema",      "Entertainment");
        KEYWORD_MAP.put("pvr",         "Entertainment");
        KEYWORD_MAP.put("inox",        "Entertainment");
        KEYWORD_MAP.put("bookmyshow",  "Entertainment");
        KEYWORD_MAP.put("game",        "Entertainment");

        // Shopping
        KEYWORD_MAP.put("amazon",      "Shopping");
        KEYWORD_MAP.put("flipkart",    "Shopping");
        KEYWORD_MAP.put("myntra",      "Shopping");
        KEYWORD_MAP.put("ajio",        "Shopping");
        KEYWORD_MAP.put("meesho",      "Shopping");
        KEYWORD_MAP.put("nykaa",       "Shopping");
        KEYWORD_MAP.put("snapdeal",    "Shopping");
        KEYWORD_MAP.put("tatacliq",    "Shopping");
        KEYWORD_MAP.put("clothes",     "Shopping");
        KEYWORD_MAP.put("shopping",    "Shopping");

        // Health
        KEYWORD_MAP.put("hospital",    "Health");
        KEYWORD_MAP.put("clinic",      "Health");
        KEYWORD_MAP.put("pharmacy",    "Health");
        KEYWORD_MAP.put("medplus",     "Health");
        KEYWORD_MAP.put("apollo",      "Health");
        KEYWORD_MAP.put("doctor",      "Health");
        KEYWORD_MAP.put("medicine",    "Health");
        KEYWORD_MAP.put("health",      "Health");
        KEYWORD_MAP.put("gym",         "Health");
        KEYWORD_MAP.put("fitness",     "Health");
        KEYWORD_MAP.put("lab",         "Health");
        KEYWORD_MAP.put("diagnostic",  "Health");

        // Education
        KEYWORD_MAP.put("udemy",       "Education");
        KEYWORD_MAP.put("coursera",    "Education");
        KEYWORD_MAP.put("unacademy",   "Education");
        KEYWORD_MAP.put("byju",        "Education");
        KEYWORD_MAP.put("college",     "Education");
        KEYWORD_MAP.put("school",      "Education");
        KEYWORD_MAP.put("tuition",     "Education");
        KEYWORD_MAP.put("fees",        "Education");
        KEYWORD_MAP.put("books",       "Education");
        KEYWORD_MAP.put("stationery",  "Education");

        // Salary
        KEYWORD_MAP.put("salary",      "Salary");
        KEYWORD_MAP.put("freelance",   "Salary");
        KEYWORD_MAP.put("stipend",     "Salary");
        KEYWORD_MAP.put("payment received", "Salary");
        KEYWORD_MAP.put("credited by", "Salary");
        KEYWORD_MAP.put("transfer from", "Salary");
        KEYWORD_MAP.put("neft",        "Salary");
        KEYWORD_MAP.put("imps",        "Salary");
    }

    private static final List<String> CATEGORIES = List.of(
            "Food", "Travel", "Bills", "Entertainment",
            "Shopping", "Health", "Education", "Salary", "Other"
    );

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public Transaction addTransaction(String userId, TransactionRequest req) {
        String category = (req.getCategory() != null && !req.getCategory().isBlank())
                ? req.getCategory()
                : autoCategory(req.getDescription());

        Transaction txn = Transaction.builder()
                .userId(userId)
                .amount(req.getAmount())
                .type(req.getType())
                .category(category)
                .description(req.getDescription())
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .build();

        return transactionRepository.save(txn);
    }

    public List<Transaction> getAllTransactions(String userId) {
        return transactionRepository.findByUserIdOrderByDateDesc(userId);
    }

    public Transaction getTransaction(String userId, String id) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!txn.getUserId().equals(userId)) {
            throw new RuntimeException("Transaction not found");
        }
        return txn;
    }

    @Transactional
    public Transaction updateTransaction(String userId, String id, TransactionRequest req) {
        Transaction txn = getTransaction(userId, id);

        txn.setAmount(req.getAmount());
        txn.setType(req.getType());
        txn.setDate(req.getDate());
        txn.setDescription(req.getDescription());

        String category = (req.getCategory() != null && !req.getCategory().isBlank())
                ? req.getCategory()
                : autoCategory(req.getDescription());
        txn.setCategory(category);

        return transactionRepository.save(txn);
    }

    @Transactional
    public void deleteTransaction(String userId, String id) {
        if (!transactionRepository.existsByIdAndUserId(id, userId)) {
            throw new RuntimeException("Transaction not found");
        }
        transactionRepository.deleteByIdAndUserId(id, userId);
    }

    public List<Transaction> getByCategory(String userId, String category) {
        return transactionRepository.findByUserIdAndCategoryIgnoreCaseOrderByDateDesc(userId, category);
    }

    public List<Transaction> getByType(String userId, String type) {
        return transactionRepository.findByUserIdAndTypeOrderByDateDesc(userId, type);
    }

    public List<Transaction> getByDateRange(String userId, LocalDate from, LocalDate to) {
        return transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);
    }

    public List<String> getCategories() {
        return CATEGORIES;
    }

    // ── Auto-categorisation ───────────────────────────────────────────────────

    public String autoCategory(String description) {
        if (description == null || description.isBlank()) return "Other";
        String lower = description.toLowerCase();
        for (Map.Entry<String, String> entry : KEYWORD_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return "Other";
    }

    // ── Dashboard Summary ─────────────────────────────────────────────────────

    public Map<String, Object> getDashboardSummary(String userId) {
        List<Transaction> all = transactionRepository.findByUserIdOrderByDateDesc(userId);

        double totalIncome = all.stream()
                .filter(t -> "income".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();

        double totalExpense = all.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();

        Map<String, Double> categoryBreakdown = all.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType()))
                .collect(groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "Other",
                        summingDouble(Transaction::getAmount)
                ));

        List<Transaction> recent = all.stream().limit(5).collect(toList());

        Map<String, Double> monthlyTrend = all.stream()
                .filter(t -> "expense".equalsIgnoreCase(t.getType())
                        && t.getDate() != null
                        && t.getDate().getYear() == LocalDate.now().getYear())
                .collect(groupingBy(
                        t -> t.getDate().getMonth().toString(),
                        summingDouble(Transaction::getAmount)
                ));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalIncome",       totalIncome);
        summary.put("totalExpense",      totalExpense);
        summary.put("balance",           totalIncome - totalExpense);
        summary.put("categoryBreakdown", categoryBreakdown);
        summary.put("recentTransactions",recent);
        summary.put("monthlyTrend",      monthlyTrend);
        summary.put("transactionCount",  all.size());

        return summary;
    }
}
