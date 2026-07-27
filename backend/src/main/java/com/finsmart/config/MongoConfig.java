package com.finsmart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * Fixes the LocalDate timezone shift bug in Spring Data MongoDB.
 *
 * ROOT CAUSE:
 * By default, Spring Data MongoDB converts LocalDate to java.util.Date using
 * the JVM's default timezone (IST = UTC+5:30). So LocalDate "2026-07-22"
 * becomes 2026-07-21T18:30:00 UTC in the database — shifted back by one day.
 * The Health Score and AI feature then query by date range and miss records
 * that appear to be from the previous month.
 *
 * FIX:
 * Register custom converters that always use UTC midnight for LocalDate,
 * so "2026-07-22" is stored as 2026-07-22T00:00:00 UTC and read back as
 * 2026-07-22 — no shift, regardless of server timezone.
 *
 * NOTE: This only fixes NEW documents going forward. Existing documents with
 * shifted dates need to be deleted and re-imported after this fix is active.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new LocalDateToDateConverter(),
                new DateToLocalDateConverter()
        ));
    }

    /**
     * Converts LocalDate → Date using UTC midnight.
     * e.g. 2026-07-22 → 2026-07-22T00:00:00.000Z (not 2026-07-21T18:30:00Z)
     */
    static class LocalDateToDateConverter implements Converter<LocalDate, Date> {
        @Override
        public Date convert(LocalDate source) {
            return Date.from(source.atStartOfDay(ZoneOffset.UTC).toInstant());
        }
    }

    /**
     * Converts Date → LocalDate using UTC, so the same date comes back out.
     */
    static class DateToLocalDateConverter implements Converter<Date, LocalDate> {
        @Override
        public LocalDate convert(Date source) {
            return Instant.ofEpochMilli(source.getTime())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
        }
    }
}