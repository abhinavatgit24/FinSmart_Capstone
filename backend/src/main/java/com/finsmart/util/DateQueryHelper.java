package com.finsmart.util;

import java.time.LocalDate;

/**
 * Date boundaries for the application's business calendar.
 *
 * LocalDate is persisted at UTC midnight by MongoConfig. Queries must therefore
 * use their exact boundaries: widening them by a day mixes transactions from
 * adjacent months and produces incorrect budgets, health scores and AI answers.
 */
public class DateQueryHelper {

    /**
     * Returns the exact first day of the current month.
     */
    public static LocalDate monthStart() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * Returns today, the last date that can contain current data.
     */
    public static LocalDate monthEnd() {
        return LocalDate.now();
    }

    /**
     * Returns the exact start of a custom period.
     */
    public static LocalDate periodStart(LocalDate from) {
        return from;
    }

    /**
     * Returns the exact end of a custom period.
     */
    public static LocalDate periodEnd(LocalDate to) {
        return to;
    }
}
